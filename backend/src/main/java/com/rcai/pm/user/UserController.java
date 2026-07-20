package com.rcai.pm.user;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departments;
    private final AuditService audit;

    public UserController(UserAccountRepository users, PasswordEncoder passwordEncoder, DepartmentRepository departments,
                          AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.departments = departments;
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'DEPARTMENT_MANAGER')")
    public List<UserSummary> list(Authentication authentication) {
        UserAccount actor = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        List<UserAccount> visible = actor.getRoles().contains(Role.ADMIN) || actor.getRoles().contains(Role.PROJECT_MANAGER)
            ? users.findAllByOrderByDisplayNameAsc()
            : actor.getDepartmentId() == null ? List.of()
            : users.findDepartmentUsers(actor.getDepartmentId());
        return visible.stream().map(UserSummary::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary create(@Valid @RequestBody CreateUser request, Authentication authentication) {
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "账号已存在");
        }
        if (request.userType() == UserType.CUSTOMER && !request.roles().equals(Set.of(Role.CUSTOMER))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "客户账号只能使用客户角色");
        }
        if (request.userType() == UserType.INTERNAL && request.roles().contains(Role.CUSTOMER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "内部账号不能使用客户角色");
        }
        if (request.userType() == UserType.CUSTOMER && request.departmentId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "客户账号不能归属内部部门");
        }
        Department department = request.departmentId() == null ? null : departments.findById(request.departmentId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "部门不存在"));
        UserAccount account = new UserAccount(
            request.username().trim(),
            passwordEncoder.encode(request.password()),
            request.displayName().trim(),
            request.email(),
            request.userType(),
            request.roles(),
            department
        );
        account = users.save(account);
        audit.log(authentication, "USER_CREATED", "USER", account.getId(), Map.of(
            "username", account.getUsername(), "userType", account.getUserType().name()
        ));
        return UserSummary.from(account);
    }

    public record CreateUser(
        @NotBlank String username,
        @Size(min = 10, message = "密码至少 10 位") String password,
        @NotBlank String displayName,
        @Email String email,
        @NotNull UserType userType,
        @NotEmpty Set<Role> roles,
        Long departmentId
    ) {}

    public record UserSummary(Long id, String username, String displayName, String email, UserType userType,
                              Set<Role> roles, Long departmentId, String departmentName) {
        static UserSummary from(UserAccount account) {
            Department department = account.getDepartment();
            return new UserSummary(account.getId(), account.getUsername(), account.getDisplayName(), account.getEmail(),
                account.getUserType(), account.getRoles(), account.getDepartmentId(),
                department == null ? null : department.getName());
        }
    }
}
