package com.rcai.pm.requirement;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.notification.NotificationService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.Priority;
import com.rcai.pm.project.Project;
import com.rcai.pm.project.ProjectRepository;
import com.rcai.pm.project.ProjectService;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@Transactional(readOnly = true)
public class RequirementService {
    private final RequirementRepository requirements;
    private final UserAccountRepository users;
    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final AuditService audit;
    private final ApprovalService approvals;
    private final NotificationService notifications;

    public RequirementService(RequirementRepository requirements, UserAccountRepository users, ProjectRepository projects,
                              ProjectService projectService, AuditService audit, ApprovalService approvals,
                              NotificationService notifications) {
        this.requirements = requirements;
        this.users = users;
        this.projects = projects;
        this.projectService = projectService;
        this.audit = audit;
        this.approvals = approvals;
        this.notifications = notifications;
    }

    public List<RequirementView> list(Authentication authentication) {
        UserAccount actor = current(authentication);
        List<Requirement> result = actor.getRoles().contains(Role.ADMIN) ? requirements.findAll() : requirements.findAccessible(actor.getId());
        return result.stream().map(RequirementView::from).toList();
    }

    @Transactional
    public RequirementView create(CreateRequirement request, Authentication authentication) {
        return createAs(request, current(authentication));
    }

    RequirementView createAs(CreateRequirement request, UserAccount actor) {
        if (request.customerId() != null && !actor.getRoles().contains(Role.ADMIN)
            && !actor.getRoles().contains(Role.PROJECT_MANAGER) && actor.getUserType() != UserType.CUSTOMER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员或项目经理可以代客户提交需求");
        }
        UserAccount customer = actor.getUserType() == UserType.CUSTOMER ? actor
            : request.customerId() == null ? null : users.findById(request.customerId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "客户账号不存在"));
        if (customer != null && customer.getUserType() != UserType.CUSTOMER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "所选账号不是客户账号");
        }
        Requirement requirement = requirements.save(new Requirement(
            uniqueNumber(), request.source(), request.title().trim(), request.description(), actor, customer,
            request.priority(), request.projectType()
        ));
        audit.log(actor, "REQUIREMENT_CREATED", "REQUIREMENT", requirement.getId(), Map.of("title", requirement.getTitle()));
        notifications.notify(List.of(actor), "REQUIREMENT_CREATED", "需求已提交", requirement.getTitle(),
            "REQUIREMENT", requirement.getId(), 0);
        return RequirementView.from(requirement);
    }

    @Transactional
    public RequirementView assign(Long id, AssignRequirement request, Authentication authentication) {
        UserAccount actor = current(authentication);
        if (!actor.getRoles().contains(Role.ADMIN) && !actor.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员或项目经理可以分派需求");
        }
        Requirement requirement = requirements.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
        requireActive(requirement);
        UserAccount assignee = users.findById(request.assigneeId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "负责人不存在"));
        if (assignee.getUserType() != UserType.INTERNAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "需求负责人必须是内部用户");
        }
        Project project = request.projectId() == null ? null : projects.findById(request.projectId())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "项目不存在"));
        if (project != null && !actor.getRoles().contains(Role.ADMIN)
            && !project.getManager().getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "项目经理只能把需求关联到自己管理的项目");
        }
        requirement.assign(assignee, project);
        notifications.watch("REQUIREMENT", requirement.getId(), request.notifyUserIds());
        audit.log(actor, "REQUIREMENT_ASSIGNED", "REQUIREMENT", requirement.getId(), Map.of(
            "assigneeId", assignee.getId(), "projectId", String.valueOf(request.projectId())
        ));
        Set<UserAccount> recipients = requirementRecipients(requirement);
        notifications.notify(recipients, "REQUIREMENT_ASSIGNED", "需求已分派", requirement.getTitle(),
            "REQUIREMENT", requirement.getId(), 0);
        return RequirementView.from(requirement);
    }

    @Transactional
    public RequirementView transition(Long id, RequirementStatus status, String opinion, Authentication authentication) {
        Requirement active = requirements.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
        requireActive(active);
        if (status == RequirementStatus.PENDING_APPROVAL) {
            approvals.submit(id, authentication);
            return RequirementView.from(requirements.findById(id).orElseThrow());
        }
        if (status == RequirementStatus.APPROVED || status == RequirementStatus.REJECTED) {
            approvals.decide(id, new ApprovalService.DecisionRequest(
                status == RequirementStatus.APPROVED ? ApprovalDecision.APPROVED : ApprovalDecision.REJECTED, opinion
            ), authentication);
            return RequirementView.from(requirements.findById(id).orElseThrow());
        }
        UserAccount actor = current(authentication);
        Requirement requirement = requirements.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
        RequirementStatus current = requirement.getStatus();
        boolean admin = actor.getRoles().contains(Role.ADMIN);
        boolean assignee = requirement.getAssignee() != null && requirement.getAssignee().getId().equals(actor.getId());
        boolean allowed = (admin || assignee) && current == RequirementStatus.REJECTED
            && status == RequirementStatus.REFINING;
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "不能执行此需求状态变更");
        requirement.changeStatus(status);
        audit.log(actor, "REQUIREMENT_STATUS_CHANGED", "REQUIREMENT", requirement.getId(), Map.of(
            "from", current.name(), "to", status.name()
        ));
        notifications.notify(requirementRecipients(requirement), "REQUIREMENT_STATUS_CHANGED", "需求状态已更新",
            requirement.getTitle() + "：" + current + " → " + status, "REQUIREMENT", requirement.getId(), 0);
        return RequirementView.from(requirement);
    }

    @Transactional
    public ProjectService.ProjectSummary createProject(Long id, CreateProjectFromRequirement request,
                                                       Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirements.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
        requireActive(requirement);
        if (requirement.getStatus() != RequirementStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "只有已批准需求可以创建项目");
        }
        if (requirement.getProject() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "需求已经关联项目");
        }
        boolean assigneeManager = requirement.getAssignee() != null
            && requirement.getAssignee().getId().equals(actor.getId())
            && actor.getRoles().contains(Role.PROJECT_MANAGER);
        if (!actor.getRoles().contains(Role.ADMIN) && !assigneeManager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员或需求负责人可以创建项目");
        }
        Long customerId = requirement.getCustomer() == null ? null : requirement.getCustomer().getId();
        ProjectService.ProjectSummary created = projectService.create(new ProjectService.CreateProject(
            request.name(), request.description(), request.projectType(), request.priority(), request.managerId(),
            customerId, request.plannedStartAt(), request.plannedEndAt()
        ), authentication);
        requirement.linkProject(projects.getReferenceById(created.id()));
        audit.log(actor, "REQUIREMENT_LINKED_TO_PROJECT", "REQUIREMENT", requirement.getId(), Map.of(
            "projectId", created.id()
        ));
        notifications.notify(requirementRecipients(requirement), "REQUIREMENT_LINKED_TO_PROJECT", "需求已关联项目",
            created.name(), "REQUIREMENT", requirement.getId(), 0);
        return created;
    }

    private void requireActive(Requirement requirement) {
        if (requirement.getMergedIntoId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "来源需求已合并，只能查看目标映射");
        }
    }

    private UserAccount current(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    private Set<UserAccount> requirementRecipients(Requirement requirement) {
        Set<UserAccount> recipients = new LinkedHashSet<>();
        recipients.add(requirement.getSubmitter());
        if (requirement.getAssignee() != null) recipients.add(requirement.getAssignee());
        if (requirement.getCustomer() != null) recipients.add(requirement.getCustomer());
        return recipients;
    }

    private String uniqueNumber() {
        String value;
        do value = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (requirements.existsByRequirementNo(value));
        return value;
    }

    public record CreateRequirement(@NotNull RequirementSource source, @NotBlank String title, String description,
                                    @NotNull Priority priority, Long customerId, @NotNull ProjectType projectType) {}
    public record AssignRequirement(@NotNull Long assigneeId, Long projectId, Set<Long> notifyUserIds) {
        public AssignRequirement(Long assigneeId, Long projectId) { this(assigneeId, projectId, Set.of()); }
    }
    public record CreateProjectFromRequirement(@NotBlank String name, String description,
                                               @NotNull ProjectType projectType, @NotNull Priority priority,
                                               @NotNull Long managerId, LocalDateTime plannedStartAt,
                                               LocalDateTime plannedEndAt) {}
    public record RequirementView(Long id, String requirementNo, RequirementSource source, String title, String description,
                                  Long submitterId, String submitterName, Long customerId, Long assigneeId,
                                  String assigneeName, Long projectId, ProjectType projectType, RequirementStatus status,
                                  Priority priority, Instant createdAt, Long mergedIntoId) {
        static RequirementView from(Requirement r) {
            return new RequirementView(r.getId(), r.getRequirementNo(), r.getSource(), r.getTitle(), r.getDescription(),
                r.getSubmitter().getId(), r.getSubmitter().getDisplayName(), r.getCustomer() == null ? null : r.getCustomer().getId(),
                r.getAssignee() == null ? null : r.getAssignee().getId(), r.getAssignee() == null ? null : r.getAssignee().getDisplayName(),
                r.getProject() == null ? null : r.getProject().getId(), r.getProjectType(), r.getStatus(), r.getPriority(), r.getCreatedAt(), r.getMergedIntoId());
        }
    }
}
