package com.rcai.pm.auth;

import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class SsoOidcUserService {
    private final UserAccountRepository users;
    private final String usernameClaim;
    private final OidcUserService delegate = new OidcUserService();

    public SsoOidcUserService(UserAccountRepository users,
                              @Value("${app.sso.username-claim:preferred_username}") String usernameClaim) {
        this.users = users;
        this.usernameClaim = usernameClaim;
    }

    public OidcUser loadUser(OidcUserRequest request) {
        return map(delegate.loadUser(request));
    }

    public OidcUser map(OidcUser external) {
        String username = external.getClaimAsString(usernameClaim);
        if (username == null || username.isBlank()) {
            throw denied("身份令牌缺少账号字段 " + usernameClaim);
        }
        UserAccount account = users.findByUsernameIgnoreCase(username)
            .filter(UserAccount::isEnabled)
            .orElseThrow(() -> denied("该统一身份尚未在系统中创建账号"));
        var authorities = account.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList();
        return new DefaultOidcUser(authorities, external.getIdToken(), external.getUserInfo(), usernameClaim);
    }

    private OAuth2AuthenticationException denied(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("sso_account_denied"), message);
    }
}
