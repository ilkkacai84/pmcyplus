package com.rcai.pm.project;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.notification.NotificationService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserType;
import com.rcai.pm.workflow.WorkflowObjectType;
import com.rcai.pm.workflow.WorkflowService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final MilestoneRepository milestones;
    private final TaskItemRepository tasks;
    private final WorklogRepository worklogs;
    private final DeliveryVersionRepository deliveries;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final WorkflowService workflow;
    private final NotificationService notifications;

    public ProjectService(ProjectRepository projects, ProjectMemberRepository members, MilestoneRepository milestones,
                          TaskItemRepository tasks, WorklogRepository worklogs, DeliveryVersionRepository deliveries,
                          UserAccountRepository users, AuditService audit, WorkflowService workflow,
                          NotificationService notifications) {
        this.projects = projects;
        this.members = members;
        this.milestones = milestones;
        this.tasks = tasks;
        this.worklogs = worklogs;
        this.deliveries = deliveries;
        this.users = users;
        this.audit = audit;
        this.workflow = workflow;
        this.notifications = notifications;
    }

    public List<ProjectSummary> list(Authentication authentication) {
        UserAccount user = current(authentication);
        List<Project> result = user.getRoles().contains(Role.ADMIN) ? projects.findAll() : projects.findAccessible(user.getId());
        return result.stream().map(ProjectSummary::from).toList();
    }

    public ProjectDetails get(Long projectId, Authentication authentication) {
        Project project = accessibleProject(projectId, authentication);
        List<MilestoneView> milestoneViews = milestones.findByProjectIdOrderByPlannedAtAsc(projectId).stream().map(MilestoneView::from).toList();
        List<TaskView> taskViews = tasks.findByProjectIdAndMergedIntoIdIsNullOrderByCreatedAtAsc(projectId).stream().map(TaskView::from).toList();
        List<DeliveryView> deliveryViews = deliveries.findByProjectId(projectId).stream().map(DeliveryView::from).toList();
        return ProjectDetails.from(project, milestoneViews, taskViews, deliveryViews);
    }

    @Transactional
    public ProjectSummary create(CreateProject request, Authentication authentication) {
        UserAccount actor = current(authentication);
        requireAnyRole(actor, Role.ADMIN, Role.PROJECT_MANAGER);
        UserAccount manager = request.managerId() == null ? actor : user(request.managerId());
        UserAccount customer = request.customerId() == null ? null : user(request.customerId());
        if (!manager.getRoles().contains(Role.ADMIN) && !manager.getRoles().contains(Role.PROJECT_MANAGER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "项目经理账号缺少项目管理角色");
        }
        if (customer != null && customer.getUserType() != UserType.CUSTOMER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "项目客户必须是客户账号");
        }
        Project project = new Project(
            uniqueProjectCode(), request.name().trim(), request.description(), request.projectType(), request.priority(),
            manager, customer, request.plannedStartAt(), request.plannedEndAt()
        );
        project = projects.save(project);
        members.save(new ProjectMember(project.getId(), manager.getId(), "MANAGER"));
        audit.log(actor, "PROJECT_CREATED", "PROJECT", project.getId(), Map.of("name", project.getName()));
        return ProjectSummary.from(project);
    }

    @Transactional
    public ProjectSummary changeProjectStatus(Long projectId, ProjectStatus status, Authentication authentication) {
        Project project = managedProject(projectId, authentication);
        ProjectStatus previous = project.getStatus();
        project.changeStatus(status);
        audit.log(authentication, "PROJECT_STATUS_CHANGED", "PROJECT", projectId, Map.of(
            "from", previous.name(), "to", status.name()
        ));
        return ProjectSummary.from(project);
    }

    @Transactional
    public ProjectDetails updateFinancials(Long projectId, UpdateFinancials request, Authentication authentication) {
        Project project = managedProject(projectId, authentication);
        project.updateFinancials(request.budget(), request.laborCost(), request.otherCost());
        audit.log(authentication, "PROJECT_FINANCIALS_UPDATED", "PROJECT", projectId, Map.of(
            "budget", request.budget(), "laborCost", request.laborCost(), "otherCost", request.otherCost()
        ));
        return get(projectId, authentication);
    }

    @Transactional
    public MilestoneView createMilestone(Long projectId, CreateMilestone request, Authentication authentication) {
        Project project = managedProject(projectId, authentication);
        UserAccount owner = request.ownerId() == null ? project.getManager() : user(request.ownerId());
        Milestone milestone = milestones.save(new Milestone(project, request.name().trim(), owner, request.plannedAt()));
        audit.log(authentication, "MILESTONE_CREATED", "MILESTONE", milestone.getId(), Map.of("projectId", projectId));
        return MilestoneView.from(milestone);
    }

    @Transactional
    public TaskView createTask(Long projectId, CreateTask request, Authentication authentication) {
        Project project = managedProject(projectId, authentication);
        UserAccount owner = user(request.ownerId());
        Milestone milestone = request.milestoneId() == null ? null : milestones.findById(request.milestoneId())
            .filter(value -> value.getProject().getId().equals(projectId))
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "里程碑不属于当前项目"));
        TaskItem parent = request.parentTaskId() == null ? null : tasks.findById(request.parentTaskId())
            .filter(value -> value.getProject().getId().equals(projectId))
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "父任务不属于当前项目"));
        TaskItem task = tasks.save(new TaskItem(project, milestone, parent, request.title().trim(), request.description(), owner,
            request.priority(), request.plannedStartAt(), request.plannedEndAt(), request.estimatedHours()));
        if (!members.existsByProjectIdAndUserId(projectId, owner.getId())) {
            members.save(new ProjectMember(projectId, owner.getId(), "MEMBER"));
        }
        audit.log(authentication, "TASK_CREATED", "TASK", task.getId(), Map.of(
            "projectId", projectId, "ownerId", owner.getId()
        ));
        notifications.notify(List.of(owner), "TASK_ASSIGNED", "新任务已分配给你", task.getTitle(),
            "TASK", task.getId(), 0);
        return TaskView.from(task);
    }

    @Transactional
    public TaskView transitionTask(Long taskId, TaskStatus target, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        accessibleProject(task.getProject().getId(), authentication);
        if (target == TaskStatus.PENDING_ACCEPTANCE || target == TaskStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "提交和验收必须使用交付接口");
        }
        boolean manager = task.getProject().getManager().getId().equals(actor.getId());
        boolean admin = actor.getRoles().contains(Role.ADMIN);
        boolean owner = task.getOwner().getId().equals(actor.getId());
        TaskStatus current = task.getStatus();
        if (!admin && !manager && !owner) throw new ApiException(HttpStatus.FORBIDDEN, "当前用户无权修改该任务");
        workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
            current.name(), target.name(), actor, null);
        task.changeStatus(target);
        audit.log(actor, "TASK_STATUS_CHANGED", "TASK", taskId, Map.of(
            "from", current.name(), "to", target.name()
        ));
        return TaskView.from(task);
    }

    @Transactional
    public TaskView completeWithWorklog(Long taskId, CompleteTask request, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        accessibleProject(task.getProject().getId(), authentication);
        boolean allowed = actor.getRoles().contains(Role.ADMIN)
            || task.getProject().getManager().getId().equals(actor.getId())
            || task.getOwner().getId().equals(actor.getId());
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "只有负责人或项目经理可以提交任务");
        if (!List.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED).contains(task.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "只有进行中或阻塞的任务可以提交交付");
        }
        workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
            task.getStatus().name(), TaskStatus.PENDING_ACCEPTANCE.name(), actor, request.note());
        worklogs.save(new Worklog(task, actor, request.hours(), request.note(), request.workedOn()));
        task.addActualHours(request.hours());
        task.changeStatus(TaskStatus.PENDING_ACCEPTANCE);
        DeliveryVersion delivery = deliveries.save(new DeliveryVersion(
            task, deliveries.findMaxVersionNo(taskId) + 1, actor, request.note()
        ));
        audit.log(actor, "TASK_DELIVERED", "TASK", taskId, Map.of(
            "version", delivery.getVersionNo(), "hours", request.hours()
        ));
        if (task.getProject().getCustomer() != null) {
            notifications.notify(List.of(task.getProject().getCustomer()), "DELIVERY_SUBMITTED", "有新的交付待验收",
                task.getTitle(), "TASK", taskId, 0);
        }
        return TaskView.from(task);
    }

    @Transactional
    public DeliveryView reviewDelivery(Long taskId, ReviewDelivery request, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        accessibleProject(task.getProject().getId(), authentication);
        boolean customer = task.getProject().getCustomer() != null
            && task.getProject().getCustomer().getId().equals(actor.getId());
        if (!customer) throw new ApiException(HttpStatus.FORBIDDEN, "只有项目关联客户可以验收交付");
        if (task.getStatus() != TaskStatus.PENDING_ACCEPTANCE) {
            throw new ApiException(HttpStatus.CONFLICT, "当前任务不在待验收状态");
        }
        if (request.decision() == DeliveryStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请选择有效的验收结果");
        }
        if (request.decision() != DeliveryStatus.ACCEPTED
            && (request.opinion() == null || request.opinion().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "驳回或要求修改时必须填写验收意见");
        }
        DeliveryVersion delivery = deliveries.findFirstByTaskIdAndStatusOrderByVersionNoDesc(taskId, DeliveryStatus.PENDING)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "未找到待验收交付版本"));
        TaskStatus target = request.decision() == DeliveryStatus.ACCEPTED
            ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS;
        workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
            task.getStatus().name(), target.name(), actor, request.opinion());
        delivery.review(request.decision(), actor, request.opinion());
        task.changeStatus(target);
        audit.log(actor, "DELIVERY_REVIEWED", "TASK", taskId, Map.of(
            "version", delivery.getVersionNo(), "decision", request.decision().name(),
            "opinion", request.opinion() == null ? "" : request.opinion()
        ));
        notifications.notify(List.of(task.getOwner(), task.getProject().getManager()), "DELIVERY_REVIEWED",
            "客户已完成交付验收", request.decision().name(), "TASK", taskId, 0);
        return DeliveryView.from(delivery);
    }

    private Project accessibleProject(Long id, Authentication authentication) {
        UserAccount user = current(authentication);
        Project project = projects.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "项目不存在"));
        boolean access = user.getRoles().contains(Role.ADMIN)
            || project.getManager().getId().equals(user.getId())
            || (project.getCustomer() != null && project.getCustomer().getId().equals(user.getId()))
            || members.existsByProjectIdAndUserId(id, user.getId());
        if (!access) throw new ApiException(HttpStatus.FORBIDDEN, "无权查看此项目");
        return project;
    }

    private Project managedProject(Long id, Authentication authentication) {
        UserAccount user = current(authentication);
        Project project = projects.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "项目不存在"));
        if (!user.getRoles().contains(Role.ADMIN) && !project.getManager().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有项目经理或管理员可以管理项目");
        }
        return project;
    }

    private UserAccount current(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    private UserAccount user(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "用户不存在"));
    }

    private void requireAnyRole(UserAccount user, Role... roles) {
        if (java.util.Arrays.stream(roles).noneMatch(user.getRoles()::contains)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "当前角色不能创建项目");
        }
    }

    private String uniqueProjectCode() {
        String code;
        do code = "PRJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (projects.existsByCode(code));
        return code;
    }

    public record CreateProject(@NotBlank String name, String description, @NotNull ProjectType projectType,
                                @NotNull Priority priority, Long managerId, Long customerId,
                                LocalDateTime plannedStartAt, LocalDateTime plannedEndAt) {}
    public record CreateMilestone(@NotBlank String name, Long ownerId, LocalDateTime plannedAt) {}
    public record CreateTask(@NotBlank String title, String description, @NotNull Long ownerId, Long milestoneId,
                             Long parentTaskId, @NotNull Priority priority, LocalDateTime plannedStartAt,
                             LocalDateTime plannedEndAt, @DecimalMin("0") BigDecimal estimatedHours) {}
    public record CompleteTask(@NotNull @DecimalMin("0.01") BigDecimal hours, String note, @NotNull LocalDate workedOn) {}
    public record ReviewDelivery(@NotNull DeliveryStatus decision, String opinion) {}
    public record UpdateFinancials(@NotNull @DecimalMin("0") BigDecimal budget,
                                   @NotNull @DecimalMin("0") BigDecimal laborCost,
                                   @NotNull @DecimalMin("0") BigDecimal otherCost) {}

    public record ProjectSummary(Long id, String code, String name, ProjectType projectType, ProjectStatus status,
                                 Priority priority, Long managerId, String managerName, LocalDateTime plannedEndAt) {
        static ProjectSummary from(Project p) {
            return new ProjectSummary(p.getId(), p.getCode(), p.getName(), p.getProjectType(), p.getStatus(), p.getPriority(),
                p.getManager().getId(), p.getManager().getDisplayName(), p.getPlannedEndAt());
        }
    }

    public record ProjectDetails(ProjectSummary project, String description, BigDecimal budget, BigDecimal laborCost,
                                 BigDecimal otherCost, List<MilestoneView> milestones, List<TaskView> tasks,
                                 List<DeliveryView> deliveries) {
        static ProjectDetails from(Project p, List<MilestoneView> milestones, List<TaskView> tasks, List<DeliveryView> deliveries) {
            return new ProjectDetails(ProjectSummary.from(p), p.getDescription(), p.getBudget(), p.getLaborCost(),
                p.getOtherCost(), milestones, tasks, deliveries);
        }
    }

    public record MilestoneView(Long id, String name, Long ownerId, String ownerName, LocalDateTime plannedAt, ProjectStatus status) {
        static MilestoneView from(Milestone m) {
            return new MilestoneView(m.getId(), m.getName(), m.getOwner() == null ? null : m.getOwner().getId(),
                m.getOwner() == null ? null : m.getOwner().getDisplayName(), m.getPlannedAt(), m.getStatus());
        }
    }

    public record TaskView(Long id, Long milestoneId, Long parentTaskId, String title, String description, Long ownerId,
                           String ownerName, TaskStatus status, Priority priority, LocalDateTime plannedStartAt,
                           LocalDateTime plannedEndAt, LocalDateTime actualStartAt, LocalDateTime actualEndAt,
                           BigDecimal estimatedHours, BigDecimal actualHours) {
        static TaskView from(TaskItem t) {
            return new TaskView(t.getId(), t.getMilestone() == null ? null : t.getMilestone().getId(),
                t.getParentTask() == null ? null : t.getParentTask().getId(), t.getTitle(), t.getDescription(),
                t.getOwner().getId(), t.getOwner().getDisplayName(), t.getStatus(), t.getPriority(),
                t.getPlannedStartAt(), t.getPlannedEndAt(), t.getActualStartAt(), t.getActualEndAt(),
                t.getEstimatedHours(), t.getActualHours());
        }
    }

    public record DeliveryView(Long id, Long taskId, int versionNo, Long submittedBy, String submitterName,
                               String submissionNote, DeliveryStatus status, Long reviewedBy, String reviewerName,
                               String reviewOpinion, java.time.Instant submittedAt, java.time.Instant reviewedAt) {
        static DeliveryView from(DeliveryVersion d) {
            return new DeliveryView(d.getId(), d.getTask().getId(), d.getVersionNo(), d.getSubmittedBy().getId(),
                d.getSubmittedBy().getDisplayName(), d.getSubmissionNote(), d.getStatus(),
                d.getReviewedBy() == null ? null : d.getReviewedBy().getId(),
                d.getReviewedBy() == null ? null : d.getReviewedBy().getDisplayName(), d.getReviewOpinion(),
                d.getSubmittedAt(), d.getReviewedAt());
        }
    }
}
