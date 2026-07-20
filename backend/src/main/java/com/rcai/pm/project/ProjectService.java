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
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final MilestoneRepository milestones;
    private final TaskItemRepository tasks;
    private final TaskParticipantRepository participants;
    private final WorklogRepository worklogs;
    private final DeliveryVersionRepository deliveries;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final WorkflowService workflow;
    private final NotificationService notifications;

    public ProjectService(ProjectRepository projects, ProjectMemberRepository members, MilestoneRepository milestones,
                          TaskItemRepository tasks, TaskParticipantRepository participants,
                          WorklogRepository worklogs, DeliveryVersionRepository deliveries,
                          UserAccountRepository users, AuditService audit, WorkflowService workflow,
                          NotificationService notifications) {
        this.projects = projects;
        this.members = members;
        this.milestones = milestones;
        this.tasks = tasks;
        this.participants = participants;
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
        UserAccount actor = current(authentication);
        List<MilestoneView> milestoneViews = milestones.findByProjectIdOrderByPlannedAtAsc(projectId).stream().map(this::milestoneView).toList();
        List<TaskView> taskViews = tasks.findByProjectIdAndMergedIntoIdIsNullOrderByCreatedAtAsc(projectId).stream()
            .map(task -> taskView(task, actor)).toList();
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
        notifications.notify(projectRecipients(project), "PROJECT_STATUS_CHANGED", "项目状态已更新",
            project.getName() + "：" + previous + " → " + status, "PROJECT", projectId, 0);
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
        notifications.notify(projectRecipients(project), "MILESTONE_CREATED", "项目新增里程碑",
            milestone.getName(), "PROJECT", projectId, 0);
        return milestoneView(milestone);
    }

    @Transactional
    public MilestoneView changeMilestoneStatus(Long milestoneId, ProjectStatus status, Authentication authentication) {
        if (status == ProjectStatus.MERGED) throw new ApiException(HttpStatus.BAD_REQUEST, "里程碑不能手工设为已合并");
        Milestone milestone = milestones.findById(milestoneId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "里程碑不存在"));
        managedProject(milestone.getProject().getId(), authentication);
        ProjectStatus previous = milestone.getStatus();
        milestone.changeStatus(status);
        audit.log(authentication, "MILESTONE_STATUS_CHANGED", "MILESTONE", milestoneId,
            Map.of("from", previous.name(), "to", status.name()));
        notifications.notify(projectRecipients(milestone.getProject()), "MILESTONE_STATUS_CHANGED",
            "里程碑状态已更新", milestone.getName() + "：" + previous + " → " + status,
            "PROJECT", milestone.getProject().getId(), 0);
        return milestoneView(milestone);
    }

    @Transactional
    public TaskView createTask(Long projectId, CreateTask request, Authentication authentication) {
        Project project = managedProject(projectId, authentication);
        UserAccount actor = current(authentication);
        UserAccount owner = user(request.ownerId());
        Milestone milestone = request.milestoneId() == null ? null : milestones.findById(request.milestoneId())
            .filter(value -> value.getProject().getId().equals(projectId))
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "里程碑不属于当前项目"));
        TaskItem parent = request.parentTaskId() == null ? null : tasks.findById(request.parentTaskId())
            .filter(value -> value.getProject().getId().equals(projectId))
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "父任务不属于当前项目"));
        TaskItem task = tasks.save(new TaskItem(project, milestone, parent, request.title().trim(), request.description(), owner,
            request.priority(), request.plannedStartAt(), request.plannedEndAt(), request.estimatedHours()));
        Set<Long> participantIds = new LinkedHashSet<>(request.participantIds() == null ? Set.of() : request.participantIds());
        participantIds.add(owner.getId());
        List<UserAccount> taskUsers = users.findAllById(participantIds);
        if (taskUsers.size() != participantIds.size() || taskUsers.stream().anyMatch(user -> user.getUserType() != UserType.INTERNAL)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "任务参与人必须是有效内部账号");
        }
        for (UserAccount taskUser : taskUsers) {
            participants.save(new TaskParticipant(task.getId(), taskUser.getId(),
                taskUser.getId().equals(owner.getId()) ? "OWNER" : "PARTICIPANT",
                taskUser.getId().equals(owner.getId()) ? task.getEstimatedHours() : BigDecimal.ZERO));
            if (!members.existsByProjectIdAndUserId(projectId, taskUser.getId())) {
                members.save(new ProjectMember(projectId, taskUser.getId(), "MEMBER"));
            }
        }
        audit.log(authentication, "TASK_CREATED", "TASK", task.getId(), Map.of(
            "projectId", projectId, "ownerId", owner.getId()
        ));
        notifications.notify(taskUsers, "TASK_ASSIGNED", "新任务已分配", task.getTitle(),
            "TASK", task.getId(), 0);
        return taskView(task, actor);
    }

    @Transactional
    public TaskView transitionTask(Long taskId, TaskStatus target, Authentication authentication) {
        return transitionTask(taskId, target, null, authentication);
    }

    @Transactional
    public TaskView transitionTask(Long taskId, TaskStatus target, String reason, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        requireActive(task);
        accessibleProject(task.getProject().getId(), authentication);
        if (target == TaskStatus.PENDING_ACCEPTANCE || target == TaskStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "提交和验收必须使用交付接口");
        }
        boolean manager = task.getProject().getManager().getId().equals(actor.getId());
        boolean admin = actor.getRoles().contains(Role.ADMIN);
        boolean participant = participants.existsById(new TaskParticipantId(taskId, actor.getId()));
        TaskStatus current = task.getStatus();
        if (!admin && !manager && !participant) throw new ApiException(HttpStatus.FORBIDDEN, "当前用户无权修改该任务");
        var transition = workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
            current.name(), target.name(), actor, reason);
        task.changeStatus(target);
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("from", current.name()); detail.put("to", target.name());
        if (reason != null && !reason.isBlank()) detail.put("reason", reason.trim());
        audit.log(actor, "TASK_STATUS_CHANGED", "TASK", taskId, detail);
        notifications.notify(taskRecipients(task), event(transition, "TASK_STATUS_CHANGED"), "任务状态已更新",
            task.getTitle() + "：" + current + " → " + target, "TASK", taskId, 0);
        return taskView(task, actor);
    }

    @Transactional
    public TaskView completeWithWorklog(Long taskId, CompleteTask request, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        requireActive(task);
        accessibleProject(task.getProject().getId(), authentication);
        boolean allowed = actor.getRoles().contains(Role.ADMIN)
            || task.getProject().getManager().getId().equals(actor.getId())
            || participants.existsById(new TaskParticipantId(taskId, actor.getId()));
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "只有负责人或项目经理可以提交任务");
        if (!List.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED).contains(task.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "只有进行中或阻塞的任务可以提交交付");
        }
        var transition = workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
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
        Set<UserAccount> recipients = taskRecipients(task);
        if (task.getProject().getCustomer() != null) recipients.add(task.getProject().getCustomer());
        notifications.notify(recipients, event(transition, "DELIVERY_SUBMITTED"), "有新的交付待验收",
            task.getTitle(), "TASK", taskId, 0);
        return taskView(task, actor);
    }

    @Transactional
    public DeliveryView reviewDelivery(Long taskId, ReviewDelivery request, Authentication authentication) {
        UserAccount actor = current(authentication);
        TaskItem task = tasks.findById(taskId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        requireActive(task);
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
        var transition = workflow.requireTransition(task.getProject().getProjectType(), WorkflowObjectType.TASK,
            task.getStatus().name(), target.name(), actor, request.opinion());
        delivery.review(request.decision(), actor, request.opinion());
        task.changeStatus(target);
        audit.log(actor, "DELIVERY_REVIEWED", "TASK", taskId, Map.of(
            "version", delivery.getVersionNo(), "decision", request.decision().name(),
            "opinion", request.opinion() == null ? "" : request.opinion()
        ));
        notifications.notify(taskRecipients(task), event(transition, "DELIVERY_REVIEWED"),
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
        if (project.getMergedIntoId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "来源项目已合并，只能查看；请前往目标项目");
        }
        if (!user.getRoles().contains(Role.ADMIN) && !project.getManager().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有项目经理或管理员可以管理项目");
        }
        return project;
    }

    private void requireActive(TaskItem task) {
        if (task.getMergedIntoId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "来源任务已合并，只能查看");
        }
    }

    private TaskView taskView(TaskItem task, UserAccount actor) {
        List<Long> participantIds = participants.findByTaskIdOrderByUserId(task.getId()).stream()
            .map(TaskParticipant::getUserId).toList();
        return TaskView.from(task, users.findAllById(participantIds), workflow.allowedTransitions(
            task.getProject().getProjectType(), WorkflowObjectType.TASK, task.getStatus().name(), actor));
    }

    private MilestoneView milestoneView(Milestone milestone) {
        List<TaskItem> milestoneTasks = tasks.findByProjectIdAndMergedIntoIdIsNullOrderByCreatedAtAsc(
            milestone.getProject().getId()).stream()
            .filter(task -> task.getMilestone() != null && task.getMilestone().getId().equals(milestone.getId())).toList();
        long completed = milestoneTasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        BigDecimal rate = milestoneTasks.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(completed * 100d / milestoneTasks.size())
            .setScale(1, java.math.RoundingMode.HALF_UP);
        return MilestoneView.from(milestone, rate);
    }

    private Set<UserAccount> taskRecipients(TaskItem task) {
        Set<UserAccount> recipients = new LinkedHashSet<>();
        recipients.add(task.getProject().getManager());
        recipients.addAll(users.findAllById(participants.findByTaskIdOrderByUserId(task.getId()).stream()
            .map(TaskParticipant::getUserId).toList()));
        return recipients;
    }

    private Set<UserAccount> projectRecipients(Project project) {
        Set<UserAccount> recipients = new LinkedHashSet<>(users.findAllById(
            members.findByProjectId(project.getId()).stream().map(ProjectMember::getUserId).toList()));
        recipients.add(project.getManager());
        if (project.getCustomer() != null) recipients.add(project.getCustomer());
        return recipients;
    }

    private String event(com.rcai.pm.workflow.WorkflowTransition transition, String fallback) {
        String configured = transition.getNotificationEvent();
        return configured == null || configured.isBlank() ? fallback : configured;
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
                             LocalDateTime plannedEndAt, @DecimalMin("0") BigDecimal estimatedHours,
                             Set<Long> participantIds) {
        public CreateTask(String title, String description, Long ownerId, Long milestoneId, Long parentTaskId,
                          Priority priority, LocalDateTime plannedStartAt, LocalDateTime plannedEndAt,
                          BigDecimal estimatedHours) {
            this(title, description, ownerId, milestoneId, parentTaskId, priority, plannedStartAt,
                plannedEndAt, estimatedHours, Set.of());
        }
    }
    public record CompleteTask(@NotNull @DecimalMin("0.01") BigDecimal hours, String note, @NotNull LocalDate workedOn) {}
    public record ReviewDelivery(@NotNull DeliveryStatus decision, String opinion) {}
    public record UpdateFinancials(@NotNull @DecimalMin("0") BigDecimal budget,
                                   @NotNull @DecimalMin("0") BigDecimal laborCost,
                                   @NotNull @DecimalMin("0") BigDecimal otherCost) {}

    public record ProjectSummary(Long id, String code, String name, ProjectType projectType, ProjectStatus status,
                                 Priority priority, Long managerId, String managerName, LocalDateTime plannedEndAt,
                                 Long mergedIntoId) {
        static ProjectSummary from(Project p) {
            return new ProjectSummary(p.getId(), p.getCode(), p.getName(), p.getProjectType(), p.getStatus(), p.getPriority(),
                p.getManager().getId(), p.getManager().getDisplayName(), p.getPlannedEndAt(), p.getMergedIntoId());
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

    public record MilestoneView(Long id, String name, Long ownerId, String ownerName, LocalDateTime plannedAt,
                                LocalDateTime actualAt, ProjectStatus status, BigDecimal completionRate) {
        static MilestoneView from(Milestone m, BigDecimal completionRate) {
            return new MilestoneView(m.getId(), m.getName(), m.getOwner() == null ? null : m.getOwner().getId(),
                m.getOwner() == null ? null : m.getOwner().getDisplayName(), m.getPlannedAt(), m.getActualAt(),
                m.getStatus(), completionRate);
        }
    }

    public record TaskView(Long id, Long milestoneId, Long parentTaskId, String title, String description, Long ownerId,
                           String ownerName, TaskStatus status, Priority priority, LocalDateTime plannedStartAt,
                           LocalDateTime plannedEndAt, LocalDateTime actualStartAt, LocalDateTime actualEndAt,
                           BigDecimal estimatedHours, BigDecimal actualHours, Long mergedIntoId,
                           List<Long> participantIds, List<String> participantNames,
                           List<WorkflowService.AllowedTransition> allowedTransitions) {
        static TaskView from(TaskItem t, List<UserAccount> participants,
                             List<WorkflowService.AllowedTransition> allowedTransitions) {
            return new TaskView(t.getId(), t.getMilestone() == null ? null : t.getMilestone().getId(),
                t.getParentTask() == null ? null : t.getParentTask().getId(), t.getTitle(), t.getDescription(),
                t.getOwner().getId(), t.getOwner().getDisplayName(), t.getStatus(), t.getPriority(),
                t.getPlannedStartAt(), t.getPlannedEndAt(), t.getActualStartAt(), t.getActualEndAt(),
                t.getEstimatedHours(), t.getActualHours(), t.getMergedIntoId(),
                participants.stream().map(UserAccount::getId).toList(),
                participants.stream().map(UserAccount::getDisplayName).toList(), allowedTransitions);
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
