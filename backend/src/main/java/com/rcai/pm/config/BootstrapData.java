package com.rcai.pm.config;

import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapData implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public BootstrapData(
        UserAccountRepository users,
        PasswordEncoder passwordEncoder,
        @Value("${app.bootstrap-admin.username}") String username,
        @Value("${app.bootstrap-admin.password}") String password,
        @Value("${app.bootstrap-admin.display-name}") String displayName
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!users.existsByUsernameIgnoreCase(username)) {
            users.save(new UserAccount(
                username,
                passwordEncoder.encode(password),
                displayName,
                UserType.INTERNAL,
                Set.of(Role.ADMIN, Role.PROJECT_MANAGER)
            ));
        }
    }
}
