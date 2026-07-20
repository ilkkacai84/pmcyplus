package com.rcai.pm.requirement;

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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RequirementService {
    private final RequirementRepository requirements;
    private final UserAccountRepository users;
    private final ProjectRepository projects;
    private final ProjectService projectService;

    public RequirementService(RequirementRepository requirements, UserAccountRepository users, ProjectRepository projects,
                              ProjectService projectService) {
        this.requirements = requirements;
        this.users = users;
        this.projects = projects;
        this.projectService = projectService;
    }

    public List<RequirementView> list(Authentication authentication) {
        UserAccount actor = current(authentication);
        List<Requirement> result = actor.getRoles().contains(Role.ADMIN) ? requirements.findAll() : requirements.findAccessible(actor.getId());
        return result.stream().map(RequirementView::from).toList();
    }

    @Transactional
    public RequirementView create(CreateRequirement request, Authentication authentication) {
        UserAccount actor = current(authentication);
        if (request.customerId() != null && !actor.getRoles().contains(Role.ADMIN)
            && !actor.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员或项目经理可以代客户提交需求");
        }
        UserAccount customer = actor.getUserType() == UserType.CUSTOMER ? actor
            : request.customerId() == null ? null : users.findById(request.customerId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "客户账号不存在"));
        if (customer != null && customer.getUserType() != UserType.CUSTOMER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "所选账号不是客户账号");
        }
        Requirement requirement = new Requirement(uniqueNumber(), request.source(), request.title().trim(), request.description(), actor, customer, request.priority());
        return RequirementView.from(requirements.save(requirement));
    }

    @Transactional
    public RequirementView assign(Long id, AssignRequirement request, Authentication authentication) {
        UserAccount actor = current(authentication);
        if (!actor.getRoles().contains(Role.ADMIN) && !actor.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员或项目经理可以分派需求");
        }
        Requirement requirement = requirements.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
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
        return RequirementView.from(requirement);
    }

    @Transactional
    public RequirementView transition(Long id, RequirementStatus status, Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirements.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
        RequirementStatus current = requirement.getStatus();
        boolean admin = actor.getRoles().contains(Role.ADMIN);
        boolean assignee = requirement.getAssignee() != null && requirement.getAssignee().getId().equals(actor.getId());
        boolean allowed = (assignee && current == RequirementStatus.REFINING && status == RequirementStatus.PENDING_APPROVAL)
            || (admin && current == RequirementStatus.PENDING_APPROVAL
                && List.of(RequirementStatus.APPROVED, RequirementStatus.REJECTED).contains(status))
            || ((admin || assignee) && current == RequirementStatus.REJECTED && status == RequirementStatus.REFINING);
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "不能执行此需求状态变更");
        requirement.changeStatus(status);
        return RequirementView.from(requirement);
    }

    @Transactional
    public ProjectService.ProjectSummary createProject(Long id, CreateProjectFromRequirement request,
                                                       Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirements.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
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
        return created;
    }

    private UserAccount current(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    private String uniqueNumber() {
        String value;
        do value = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (requirements.existsByRequirementNo(value));
        return value;
    }

    public record CreateRequirement(@NotNull RequirementSource source, @NotBlank String title, String description,
                                    @NotNull Priority priority, Long customerId) {}
    public record AssignRequirement(@NotNull Long assigneeId, Long projectId) {}
    public record CreateProjectFromRequirement(@NotBlank String name, String description,
                                               @NotNull ProjectType projectType, @NotNull Priority priority,
                                               @NotNull Long managerId, LocalDateTime plannedStartAt,
                                               LocalDateTime plannedEndAt) {}
    public record RequirementView(Long id, String requirementNo, RequirementSource source, String title, String description,
                                  Long submitterId, String submitterName, Long customerId, Long assigneeId,
                                  String assigneeName, Long projectId, RequirementStatus status, Priority priority, Instant createdAt) {
        static RequirementView from(Requirement r) {
            return new RequirementView(r.getId(), r.getRequirementNo(), r.getSource(), r.getTitle(), r.getDescription(),
                r.getSubmitter().getId(), r.getSubmitter().getDisplayName(), r.getCustomer() == null ? null : r.getCustomer().getId(),
                r.getAssignee() == null ? null : r.getAssignee().getId(), r.getAssignee() == null ? null : r.getAssignee().getDisplayName(),
                r.getProject() == null ? null : r.getProject().getId(), r.getStatus(), r.getPriority(), r.getCreatedAt());
        }
    }
}
