package com.rcai.pm.auth;

import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import com.rcai.pm.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository users;
    private final boolean ssoEnabled;
    private final String ssoRegistrationId;
    private final LoginAttemptService loginAttempts;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                          UserAccountRepository users,
                          LoginAttemptService loginAttempts,
                          @Value("${app.sso.enabled:false}") boolean ssoEnabled,
                          @Value("${app.sso.registration-id:corporate}") String ssoRegistrationId) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
        this.loginAttempts = loginAttempts;
        this.ssoEnabled = ssoEnabled;
        this.ssoRegistrationId = ssoRegistrationId;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/options")
    public LoginOptions options() {
        return new LoginOptions(ssoEnabled, ssoRegistrationId);
    }

    @PostMapping("/login")
    public CurrentUser login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String address = httpRequest.getRemoteAddr();
        loginAttempts.checkAllowed(request.username(), address);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
            );
        } catch (AuthenticationException exception) {
            loginAttempts.recordFailure(request.username(), address);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        loginAttempts.clear(request.username(), address);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        securityContextRepository.saveContext(context, httpRequest, response);
        return current(authentication);
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        return current(authentication);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private CurrentUser current(Authentication authentication) {
        UserAccount account = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        return new CurrentUser(account.getId(), account.getUsername(), account.getDisplayName(), account.getUserType(), account.getRoles());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record CurrentUser(Long id, String username, String displayName, com.rcai.pm.user.UserType userType, java.util.Set<com.rcai.pm.user.Role> roles) {}
    public record LoginOptions(boolean ssoEnabled, String registrationId) {}
}
