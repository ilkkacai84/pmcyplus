package com.rcai.pm.requirement;

import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.Priority;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@Transactional
public class ExternalIntakeService {
    private final RequirementService requirements;
    private final UserAccountRepository users;
    private final boolean enabled;
    private final String token;

    public ExternalIntakeService(RequirementService requirements, UserAccountRepository users,
                                 @Value("${app.intake.enabled:false}") boolean enabled,
                                 @Value("${app.intake.token:}") String token) {
        this.requirements = requirements;
        this.users = users;
        this.enabled = enabled;
        this.token = token;
    }

    public RequirementService.RequirementView receive(RequirementSource source, IntakeRequest request,
                                                       String suppliedToken) {
        authorize(suppliedToken);
        if (source != RequirementSource.EMAIL && source != RequirementSource.WECHAT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "外部接入仅支持邮件和企业微信");
        }
        UserAccount submitter = users.findByUsernameIgnoreCase(request.username())
            .filter(UserAccount::isEnabled)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "接入账号不存在或已停用"));
        return requirements.createAs(new RequirementService.CreateRequirement(
            source, request.title(), request.description(), request.priority(),
            submitter.getUserType() == com.rcai.pm.user.UserType.CUSTOMER ? submitter.getId() : null,
            request.projectType()
        ), submitter);
    }

    private void authorize(String suppliedToken) {
        boolean configured = enabled && !token.isBlank() && suppliedToken != null;
        boolean matches = configured && MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8));
        if (!matches) throw new ApiException(HttpStatus.UNAUTHORIZED, "外部接入凭证无效");
    }

    public record IntakeRequest(@NotBlank String username, @NotBlank String title, String description,
                                @NotNull Priority priority, @NotNull ProjectType projectType) {}
}
