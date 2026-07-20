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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository users;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository, UserAccountRepository users) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/login")
    public CurrentUser login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
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
}
