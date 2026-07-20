package com.rcai.pm.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public List<UserSummary> list() {
        return users.findAll().stream().map(UserSummary::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary create(@Valid @RequestBody CreateUser request) {
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw new com.rcai.pm.common.ApiException(org.springframework.http.HttpStatus.CONFLICT, "账号已存在");
        }
        UserAccount account = new UserAccount(
            request.username().trim(),
            passwordEncoder.encode(request.password()),
            request.displayName().trim(),
            request.userType(),
            request.roles()
        );
        return UserSummary.from(users.save(account));
    }

    public record CreateUser(
        @NotBlank String username,
        @Size(min = 10, message = "密码至少 10 位") String password,
        @NotBlank String displayName,
        @Email String email,
        @NotNull UserType userType,
        @NotEmpty Set<Role> roles
    ) {}

    public record UserSummary(Long id, String username, String displayName, UserType userType, Set<Role> roles) {
        static UserSummary from(UserAccount account) {
            return new UserSummary(account.getId(), account.getUsername(), account.getDisplayName(), account.getUserType(), account.getRoles());
        }
    }
}
