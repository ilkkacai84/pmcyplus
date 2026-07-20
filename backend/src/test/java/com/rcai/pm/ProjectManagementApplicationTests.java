package com.rcai.pm;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.auth.SsoOidcUserService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.notification.NotificationService;
import com.rcai.pm.notification.Notification;
import com.rcai.pm.notification.NotificationChannel;
import com.rcai.pm.notification.NotificationDispatcher;
import com.rcai.pm.notification.NotificationRepository;
import com.rcai.pm.notification.NotificationSender;
import com.rcai.pm.notification.NotificationStatus;
import com.rcai.pm.notification.OverdueEscalationService;
import com.rcai.pm.risk.RiskLevel;
import com.rcai.pm.risk.RiskService;
import com.rcai.pm.risk.RiskStatus;
import com.rcai.pm.document.DocumentService;
import com.rcai.pm.document.HttpFileStorage;
import com.rcai.pm.resource.ResourceService;
import com.rcai.pm.report.ReportService;
import com.rcai.pm.merge.MergeObjectType;
import com.rcai.pm.merge.MergeService;
import com.rcai.pm.merge.MergeRecordRepository;
import com.rcai.pm.merge.MergeController;
import com.rcai.pm.project.Priority;
import com.rcai.pm.project.DeliveryStatus;
import com.rcai.pm.project.ProjectService;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.project.TaskItemRepository;
import com.rcai.pm.project.Worklog;
import com.rcai.pm.project.WorklogRepository;
import com.rcai.pm.project.DeliveryVersion;
import com.rcai.pm.project.DeliveryVersionRepository;
import com.rcai.pm.requirement.RequirementService;
import com.rcai.pm.requirement.ApprovalDecision;
import com.rcai.pm.requirement.ApprovalService;
import com.rcai.pm.requirement.RequirementSource;
import com.rcai.pm.requirement.RequirementStatus;
import com.rcai.pm.requirement.RequirementRepository;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.Department;
import com.rcai.pm.user.DepartmentRepository;
import com.rcai.pm.user.DepartmentService;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserController;
import com.rcai.pm.user.UserType;
import com.rcai.pm.workflow.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ProjectManagementApplicationTests {
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private ProjectService projects;
    @Autowired
    private RequirementService requirements;
    @Autowired
    private DepartmentService departments;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserController userController;
    @Autowired
    private AuditService audit;
    @Autowired
    private WorkflowService workflow;
    @Autowired
    private ApprovalService approvals;
    @Autowired
    private NotificationService notifications;
    @Autowired
    private OverdueEscalationService overdueEscalations;
    @Autowired
    private RiskService riskService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private MergeService mergeService;
    @Autowired
    private MergeRecordRepository mergeRecords;
    @Autowired
    private TaskItemRepository taskItems;
    @Autowired
    private WorklogRepository worklogRepository;
    @Autowired
    private DeliveryVersionRepository deliveryRepository;
    @Autowired
    private RequirementRepository requirementRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MergeController mergeController;
    @Autowired
    private SsoOidcUserService ssoUsers;

    @Test
    void contextLoads() {
    }

    @Test
    @WithMockUser(username = "user-list-admin", roles = "ADMIN")
    void userListLoadsRolesOutsideRepositoryTransaction() {
        users.save(new UserAccount(
            "user-list-admin", "unused", "列表管理员", UserType.INTERNAL, Set.of(Role.ADMIN)
        ));
        var authentication = UsernamePasswordAuthenticationToken.authenticated("user-list-admin", "n/a", Set.of());
        assertThat(userController.list(authentication))
            .anySatisfy(user -> {
                assertThat(user.username()).isEqualTo("user-list-admin");
                assertThat(user.roles()).contains(Role.ADMIN);
            });
    }

    @Test
    @Transactional
    void mergePreviewShowsMigrationScopeAndFieldConflicts() {
        UserAccount manager = saveUser("merge-preview-manager", "合并预览经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(manager.getUsername(), "n/a", Set.of());
        var source = projects.create(new ProjectService.CreateProject(
            "重复项目", "来源说明", ProjectType.INTERNAL, Priority.HIGH,
            manager.getId(), null, null, null
        ), authentication);
        var target = projects.create(new ProjectService.CreateProject(
            "保留项目", "目标说明", ProjectType.INTERNAL, Priority.MEDIUM,
            manager.getId(), null, null, null
        ), authentication);
        projects.createTask(source.id(), new ProjectService.CreateTask(
            "待迁移任务", null, manager.getId(), null, null, Priority.HIGH,
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), BigDecimal.valueOf(8)
        ), authentication);

        var preview = mergeService.preview(MergeObjectType.PROJECT, List.of(source.id()), target.id());

        assertThat(preview.target().id()).isEqualTo(target.id());
        assertThat(preview.migrationScope()).containsEntry("tasks", 1L);
        assertThat(preview.conflicts()).extracting(MergeService.FieldConflict::field)
            .contains("name", "description", "priority");

        Set<String> accepted = preview.conflicts().stream()
            .map(MergeService.FieldConflict::field).collect(java.util.stream.Collectors.toSet());
        var result = mergeService.execute(MergeObjectType.PROJECT, List.of(source.id()), target.id(),
            accepted, authentication);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(mergeRecords.findById(result.mergeRecordId())).isPresent();
        assertThat(projects.get(source.id(), authentication).project().status().name()).isEqualTo("MERGED");
        assertThat(projects.get(target.id(), authentication).tasks())
            .extracting(ProjectService.TaskView::title).contains("待迁移任务");
        assertThatThrownBy(() -> projects.updateFinancials(source.id(), new ProjectService.UpdateFinancials(
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO
        ), authentication)).isInstanceOf(ApiException.class).hasMessageContaining("只");
        assertThatThrownBy(() -> mergeService.preview(MergeObjectType.PROJECT, List.of(source.id()), target.id()))
            .isInstanceOf(ApiException.class).hasMessageContaining("已经合并");
    }

    @Test
    @Transactional
    void taskMergeMovesWorklogsAndRenumbersDeliveries() {
        UserAccount admin = saveUser("task-merge-admin", "任务合并管理员", UserType.INTERNAL, Role.ADMIN);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(admin.getUsername(), "n/a", Set.of());
        var project = projects.create(new ProjectService.CreateProject(
            "任务合并项目", null, ProjectType.INTERNAL, Priority.HIGH,
            admin.getId(), null, null, null
        ), authentication);
        var sourceView = projects.createTask(project.id(), new ProjectService.CreateTask(
            "重复任务", null, admin.getId(), null, null, Priority.HIGH,
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), BigDecimal.valueOf(4)
        ), authentication);
        var targetView = projects.createTask(project.id(), new ProjectService.CreateTask(
            "保留任务", null, admin.getId(), null, null, Priority.MEDIUM,
            LocalDateTime.now(), LocalDateTime.now().plusDays(2), BigDecimal.valueOf(8)
        ), authentication);
        var source = taskItems.findById(sourceView.id()).orElseThrow();
        var target = taskItems.findById(targetView.id()).orElseThrow();
        source.addActualHours(BigDecimal.valueOf(2));
        worklogRepository.save(new Worklog(source, admin, BigDecimal.valueOf(2), "来源工时", LocalDate.now()));
        deliveryRepository.save(new DeliveryVersion(source, 1, admin, "来源版本"));
        deliveryRepository.save(new DeliveryVersion(target, 1, admin, "目标版本"));

        var preview = mergeService.preview(MergeObjectType.TASK, List.of(source.getId()), target.getId());
        Set<String> accepted = preview.conflicts().stream()
            .map(MergeService.FieldConflict::field).collect(java.util.stream.Collectors.toSet());
        mergeService.execute(MergeObjectType.TASK, List.of(source.getId()), target.getId(), accepted, authentication);

        assertThat(source.getStatus()).isEqualTo(TaskStatus.MERGED);
        assertThat(source.getMergedIntoId()).isEqualTo(target.getId());
        assertThat(target.getActualHours()).isEqualByComparingTo("2");
        assertThat(jdbc.queryForObject("select count(*) from worklogs where task_id = ?", Long.class, target.getId()))
            .isEqualTo(1L);
        assertThat(jdbc.queryForObject("select max(version_no) from delivery_versions where task_id = ?", Integer.class, target.getId()))
            .isEqualTo(2);
        assertThatThrownBy(() -> projects.transitionTask(source.getId(), TaskStatus.TODO, authentication))
            .isInstanceOf(ApiException.class).hasMessageContaining("只");
    }

    @Test
    @Transactional
    void requirementMergePreservesSourceMapping() {
        UserAccount admin = saveUser("requirement-merge-admin", "需求合并管理员", UserType.INTERNAL, Role.ADMIN);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(admin.getUsername(), "n/a", Set.of());
        var source = requirements.create(new RequirementService.CreateRequirement(
            RequirementSource.WEB, "重复需求", "来源", Priority.HIGH, null, ProjectType.INTERNAL
        ), authentication);
        var target = requirements.create(new RequirementService.CreateRequirement(
            RequirementSource.WECHAT, "保留需求", "目标", Priority.MEDIUM, null, ProjectType.INTERNAL
        ), authentication);
        var preview = mergeService.preview(MergeObjectType.REQUIREMENT, List.of(source.id()), target.id());
        Set<String> accepted = preview.conflicts().stream()
            .map(MergeService.FieldConflict::field).collect(java.util.stream.Collectors.toSet());

        mergeService.execute(MergeObjectType.REQUIREMENT, List.of(source.id()), target.id(), accepted, authentication);

        var merged = requirementRepository.findById(source.id()).orElseThrow();
        assertThat(merged.getStatus()).isEqualTo(RequirementStatus.MERGED);
        assertThat(merged.getMergedIntoId()).isEqualTo(target.id());
        assertThatThrownBy(() -> requirements.assign(source.id(), new RequirementService.AssignRequirement(
            admin.getId(), null
        ), authentication)).isInstanceOf(ApiException.class).hasMessageContaining("只");
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void nonAdminCannotPreviewMerge() {
        assertThatThrownBy(() -> mergeController.preview(new MergeController.PreviewRequest(
            MergeObjectType.PROJECT, List.of(1L), 2L
        ))).isInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class);
    }

    @Test
    void notificationDispatcherMarksSuccessfulDelivery() {
        UserAccount recipient = new UserAccount(
            "notify-user", "unused", "通知用户", UserType.INTERNAL, Set.of(Role.MEMBER)
        );
        Notification notification = new Notification(recipient, NotificationChannel.TEAMS,
            "TASK_UPDATED", "任务更新", "内容", "TASK", 1L, 0);
        NotificationSender sender = sender(NotificationChannel.TEAMS, false);

        new NotificationDispatcher(null, List.of(sender)).dispatch(List.of(notification));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    void notificationDispatcherStopsAfterThreeFailures() {
        UserAccount recipient = new UserAccount(
            "notify-failure-user", "unused", "失败通知用户", UserType.INTERNAL, Set.of(Role.MEMBER)
        );
        Notification notification = new Notification(recipient, NotificationChannel.WECHAT,
            "TASK_UPDATED", "任务更新", "内容", "TASK", 1L, 0);
        NotificationDispatcher dispatcher = new NotificationDispatcher(null,
            List.of(sender(NotificationChannel.WECHAT, true)));

        dispatcher.dispatch(List.of(notification)); dispatcher.dispatch(List.of(notification));
        dispatcher.dispatch(List.of(notification));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttemptCount()).isEqualTo(3);
        assertThat(notification.getLastError()).contains("模拟发送失败");
    }

    @Test
    @Transactional
    void ssoMapsOnlyPrecreatedLocalAccountAndRoles() {
        saveUser("oidc-admin", "统一身份管理员", UserType.INTERNAL, Role.ADMIN);
        OidcIdToken token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(300), Map.of(
            "sub", "external-123", "preferred_username", "oidc-admin"
        ));
        DefaultOidcUser external = new DefaultOidcUser(
            List.of(new SimpleGrantedAuthority("OIDC_USER")), token, "preferred_username"
        );

        var mapped = ssoUsers.map(external);

        assertThat(mapped.getName()).isEqualTo("oidc-admin");
        assertThat(mapped.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
    }

    @Test
    void httpFileStorageUploadsAndDownloadsWithBearerToken() throws Exception {
        AtomicReference<byte[]> stored = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/files", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            if (exchange.getRequestMethod().equals("PUT")) {
                stored.set(exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(201, -1);
            } else {
                byte[] body = stored.get();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try {
            HttpFileStorage storage = new HttpFileStorage(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/files", "secret-token"
            );
            MockMultipartFile file = new MockMultipartFile(
                "file", "proof.txt", "text/plain", "external-storage".getBytes(StandardCharsets.UTF_8)
            );

            String key = storage.save(file);

            assertThat(new String(storage.load(key).getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("external-storage");
            assertThat(authorization.get()).isEqualTo("Bearer secret-token");
        } finally {
            server.stop(0);
        }
    }

    private NotificationSender sender(NotificationChannel channel, boolean fail) {
        return new NotificationSender() {
            @Override public NotificationChannel channel() { return channel; }
            @Override public boolean configured() { return true; }
            @Override public void send(Notification notification) {
                if (fail) throw new IllegalStateException("模拟发送失败");
            }
        };
    }

    @Test
    @Transactional
    void projectManagerCanCreateAndReadProject() {
        UserAccount manager = users.save(new UserAccount(
            "test-manager", "unused", "测试经理", UserType.INTERNAL, Set.of(Role.PROJECT_MANAGER)
        ));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(manager.getUsername(), "n/a", Set.of());

        var created = projects.create(new ProjectService.CreateProject(
            "测试项目", "验证核心链路", ProjectType.INTERNAL, Priority.HIGH,
            manager.getId(), null, null, null
        ), authentication);

        assertThat(created.name()).isEqualTo("测试项目");
        assertThat(projects.get(created.id(), authentication).project().managerId()).isEqualTo(manager.getId());
        var financials = projects.updateFinancials(created.id(), new ProjectService.UpdateFinancials(
            BigDecimal.valueOf(100000), BigDecimal.valueOf(32000), BigDecimal.valueOf(8000)
        ), authentication);
        assertThat(financials.budget()).isEqualByComparingTo("100000");
        assertThat(financials.laborCost().add(financials.otherCost())).isEqualByComparingTo("40000");
    }

    @Test
    @Transactional
    void deliveryKeepsVersionsAndOnlyLinkedCustomerCanReview() {
        UserAccount manager = saveUser("delivery-manager", "交付经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount customer = saveUser("delivery-customer", "验收客户", UserType.CUSTOMER, Role.CUSTOMER);
        UserAccount outsider = saveUser("delivery-outsider", "其他客户", UserType.CUSTOMER, Role.CUSTOMER);
        var managerAuth = authentication(manager);
        var customerAuth = authentication(customer);
        var outsiderAuth = authentication(outsider);

        var project = projects.create(new ProjectService.CreateProject(
            "交付版本项目", null, ProjectType.INTERNAL, Priority.HIGH,
            manager.getId(), customer.getId(), null, null
        ), managerAuth);
        var task = projects.createTask(project.id(), new ProjectService.CreateTask(
            "客户验收任务", null, manager.getId(), null, null, Priority.HIGH,
            null, null, BigDecimal.valueOf(8)
        ), managerAuth);

        projects.transitionTask(task.id(), TaskStatus.IN_PROGRESS, managerAuth);
        projects.completeWithWorklog(task.id(), new ProjectService.CompleteTask(
            BigDecimal.valueOf(4), "第一版交付", LocalDate.now()
        ), managerAuth);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> projects.reviewDelivery(
            task.id(), new ProjectService.ReviewDelivery(DeliveryStatus.ACCEPTED, null), outsiderAuth
        )).isInstanceOf(com.rcai.pm.common.ApiException.class);

        projects.reviewDelivery(task.id(), new ProjectService.ReviewDelivery(
            DeliveryStatus.CHANGES_REQUESTED, "请补充导出功能"
        ), customerAuth);
        projects.completeWithWorklog(task.id(), new ProjectService.CompleteTask(
            BigDecimal.valueOf(2), "第二版交付", LocalDate.now()
        ), managerAuth);
        projects.reviewDelivery(task.id(), new ProjectService.ReviewDelivery(
            DeliveryStatus.ACCEPTED, "验收通过"
        ), customerAuth);

        var details = projects.get(project.id(), managerAuth);
        assertThat(details.tasks().getFirst().status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(details.deliveries()).hasSize(2);
        assertThat(details.deliveries()).extracting(ProjectService.DeliveryView::versionNo).containsExactly(2, 1);
        assertThat(details.deliveries()).extracting(ProjectService.DeliveryView::status)
            .containsExactly(DeliveryStatus.ACCEPTED, DeliveryStatus.CHANGES_REQUESTED);
        var report = reportService.report(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
            ProjectType.INTERNAL, project.id(), null, null, customer.getId(), managerAuth);
        assertThat(report.completionRate()).isEqualByComparingTo("100.0");
        assertThat(report.reviewedDeliveries()).isEqualTo(2);
    }

    @Test
    @Transactional
    void requirementMustFollowAssignmentAndApprovalStateMachine() {
        UserAccount manager = saveUser("requirement-manager", "需求经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount admin = saveUser("requirement-admin", "审批管理员", UserType.INTERNAL, Role.ADMIN);
        UserAccount submitter = saveUser("requirement-member", "需求提交人", UserType.INTERNAL, Role.MEMBER);
        var managerAuth = authentication(manager);
        var adminAuth = authentication(admin);
        var submitterAuth = authentication(submitter);

        var requirement = requirements.create(new RequirementService.CreateRequirement(
            RequirementSource.WEB, "审批链路需求", "验证状态机", Priority.MEDIUM, null, ProjectType.INTERNAL
        ), submitterAuth);
        requirement = requirements.assign(requirement.id(), new RequirementService.AssignRequirement(manager.getId(), null), managerAuth);
        assertThat(requirement.status()).isEqualTo(RequirementStatus.REFINING);

        requirement = requirements.transition(requirement.id(), RequirementStatus.PENDING_APPROVAL, null, managerAuth);
        assertThat(requirement.status()).isEqualTo(RequirementStatus.PENDING_APPROVAL);
        requirement = requirements.transition(requirement.id(), RequirementStatus.APPROVED, "审批通过", adminAuth);
        assertThat(requirement.status()).isEqualTo(RequirementStatus.APPROVED);

        Long requirementId = requirement.id();
        var linkedProject = requirements.createProject(requirementId, new RequirementService.CreateProjectFromRequirement(
            "需求转项目", "由批准需求创建", ProjectType.INTERNAL, Priority.MEDIUM,
            manager.getId(), null, null
        ), adminAuth);
        assertThat(requirements.list(adminAuth).stream()
            .filter(item -> item.id().equals(requirementId))
            .findFirst().orElseThrow().projectId()).isEqualTo(linkedProject.id());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> requirements.transition(
            requirementId, RequirementStatus.REJECTED, "无权驳回", submitterAuth
        )).isInstanceOf(com.rcai.pm.common.ApiException.class);

        assertThat(audit.list()).extracting(AuditService.AuditView::action)
            .contains("REQUIREMENT_CREATED", "REQUIREMENT_ASSIGNED", "APPROVAL_SUBMITTED", "APPROVAL_DECIDED",
                "PROJECT_CREATED", "REQUIREMENT_LINKED_TO_PROJECT");
    }

    @Test
    @Transactional
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateDepartmentHierarchyAndAssignUser() {
        UserAccount departmentManager = saveUser(
            "department-manager", "部门负责人", UserType.INTERNAL, Role.DEPARTMENT_MANAGER
        );
        var headquarters = departments.create(new DepartmentService.SaveDepartment("总部", null, null));
        var delivery = departments.create(new DepartmentService.SaveDepartment(
            "交付部", headquarters.id(), departmentManager.getId()
        ));

        var member = userController.create(new UserController.CreateUser(
            "department-member", "Password@123", "部门成员", "member@example.com",
            UserType.INTERNAL, Set.of(Role.MEMBER), delivery.id()
        ), authentication(departmentManager));

        assertThat(member.departmentId()).isEqualTo(delivery.id());
        assertThat(member.departmentName()).isEqualTo("交付部");
        assertThat(departments.list()).extracting(DepartmentService.DepartmentView::name)
            .containsExactly("交付部", "总部");
    }

    @Test
    @Transactional
    @WithMockUser(roles = "DEPARTMENT_MANAGER")
    void departmentManagerOnlySeesOwnDepartmentMembersAndTasks() {
        var deliveryView = departments.create(new DepartmentService.SaveDepartment("交付部-隔离", null, null));
        var salesView = departments.create(new DepartmentService.SaveDepartment("销售部-隔离", null, null));
        Department delivery = departmentRepository.findById(deliveryView.id()).orElseThrow();
        Department sales = departmentRepository.findById(salesView.id()).orElseThrow();

        UserAccount departmentManager = users.save(new UserAccount(
            "scope-department-manager", "unused", "交付部负责人", null,
            UserType.INTERNAL, Set.of(Role.DEPARTMENT_MANAGER), delivery
        ));
        UserAccount deliveryMember = users.save(new UserAccount(
            "scope-delivery-member", "unused", "交付部成员", null,
            UserType.INTERNAL, Set.of(Role.MEMBER), delivery
        ));
        UserAccount salesMember = users.save(new UserAccount(
            "scope-sales-member", "unused", "销售部成员", null,
            UserType.INTERNAL, Set.of(Role.MEMBER), sales
        ));
        departments.update(delivery.getId(), new DepartmentService.SaveDepartment(
            delivery.getName(), null, departmentManager.getId()
        ));

        UserAccount projectManager = saveUser(
            "scope-project-manager", "隔离测试项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER
        );
        var project = projects.create(new ProjectService.CreateProject(
            "部门隔离项目", null, ProjectType.INTERNAL, Priority.MEDIUM,
            projectManager.getId(), null, null, null
        ), authentication(projectManager));
        projects.createTask(project.id(), new ProjectService.CreateTask(
            "交付部任务", null, deliveryMember.getId(), null, null, Priority.MEDIUM,
            null, null, BigDecimal.valueOf(8)
        ), authentication(projectManager));
        projects.createTask(project.id(), new ProjectService.CreateTask(
            "销售部任务", null, salesMember.getId(), null, null, Priority.MEDIUM,
            null, null, BigDecimal.valueOf(8)
        ), authentication(projectManager));

        var departmentAuth = authentication(departmentManager);
        assertThat(departments.myScope(departmentAuth).tasks())
            .extracting(DepartmentService.DepartmentTaskView::title)
            .containsExactly("交付部任务");
        assertThat(userController.list(departmentAuth))
            .extracting(UserController.UserSummary::username)
            .containsExactlyInAnyOrder("scope-department-manager", "scope-delivery-member");
    }

    @Test
    @Transactional
    void projectTypeWorkflowConfigurationControlsTaskTransitions() {
        UserAccount manager = saveUser("workflow-manager", "流程项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount member = saveUser("workflow-member", "流程项目成员", UserType.INTERNAL, Role.MEMBER);
        UserAccount admin = saveUser("workflow-admin", "流程管理员", UserType.INTERNAL, Role.ADMIN);
        var project = projects.create(new ProjectService.CreateProject(
            "流程配置项目", null, ProjectType.INTERNAL, Priority.MEDIUM,
            manager.getId(), null, null, null
        ), authentication(manager));
        var task = projects.createTask(project.id(), new ProjectService.CreateTask(
            "受流程控制的任务", null, member.getId(), null, null, Priority.MEDIUM,
            null, null, BigDecimal.ONE
        ), authentication(manager));

        var internalTemplate = workflow.list().stream()
            .filter(template -> template.projectType() == ProjectType.INTERNAL).findFirst().orElseThrow();
        var startTransition = internalTemplate.transitions().stream()
            .filter(item -> item.fromStatus().equals(TaskStatus.TODO.name())
                && item.toStatus().equals(TaskStatus.IN_PROGRESS.name()))
            .findFirst().orElseThrow();
        workflow.update(startTransition.id(), new WorkflowService.ConfigureTransition(
            Set.of(Role.ADMIN), true, false, "TASK_STARTED"
        ), authentication(admin));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> projects.transitionTask(
            task.id(), TaskStatus.IN_PROGRESS, authentication(member)
        )).isInstanceOf(com.rcai.pm.common.ApiException.class);
        assertThat(projects.transitionTask(task.id(), TaskStatus.IN_PROGRESS, authentication(admin)).status())
            .isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(workflow.list()).extracting(WorkflowService.TemplateView::projectType)
            .containsExactly(ProjectType.INTERNAL, ProjectType.TEMPORARY);
    }

    @Test
    @Transactional
    void sequentialApprovalSupportsWithdrawRejectAndResubmit() {
        UserAccount manager = saveUser("approval-manager", "审批需求负责人", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount admin = saveUser("approval-admin", "一级审批人", UserType.INTERNAL, Role.ADMIN);
        UserAccount departmentManager = saveUser(
            "approval-department-manager", "二级审批人", UserType.INTERNAL, Role.DEPARTMENT_MANAGER
        );
        UserAccount submitter = saveUser("approval-submitter", "审批提交人", UserType.INTERNAL, Role.MEMBER);
        var temporaryTemplate = workflow.list().stream()
            .filter(template -> template.projectType() == ProjectType.TEMPORARY).findFirst().orElseThrow();
        workflow.addApprovalStep(temporaryTemplate.id(), new WorkflowService.SaveApprovalStep(
            "部门负责人复核", Role.DEPARTMENT_MANAGER, true
        ), authentication(admin));

        var requirement = requirements.create(new RequirementService.CreateRequirement(
            RequirementSource.WEB, "两级审批需求", null, Priority.HIGH, null, ProjectType.TEMPORARY
        ), authentication(submitter));
        requirement = requirements.assign(requirement.id(), new RequirementService.AssignRequirement(
            manager.getId(), null
        ), authentication(manager));
        Long requirementId = requirement.id();

        approvals.submit(requirementId, authentication(manager));
        approvals.withdraw(requirementId, new ApprovalService.OpinionRequest("补充信息后重新提交"), authentication(manager));
        assertThat(requirements.list(authentication(manager)).stream()
            .filter(item -> item.id().equals(requirementId)).findFirst().orElseThrow().status())
            .isEqualTo(RequirementStatus.REFINING);

        approvals.submit(requirementId, authentication(manager));
        var afterFirstApproval = approvals.decide(requirementId, new ApprovalService.DecisionRequest(
            ApprovalDecision.APPROVED, "一级通过"
        ), authentication(admin));
        assertThat(afterFirstApproval.currentStep()).isEqualTo(2);
        assertThat(afterFirstApproval.status()).isEqualTo(com.rcai.pm.requirement.ApprovalStatus.PENDING);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> approvals.decide(
            requirementId, new ApprovalService.DecisionRequest(ApprovalDecision.REJECTED, null),
            authentication(departmentManager)
        )).isInstanceOf(com.rcai.pm.common.ApiException.class);
        approvals.decide(requirementId, new ApprovalService.DecisionRequest(
            ApprovalDecision.REJECTED, "需要补充排期"
        ), authentication(departmentManager));

        approvals.submit(requirementId, authentication(manager));
        approvals.decide(requirementId, new ApprovalService.DecisionRequest(
            ApprovalDecision.APPROVED, "一级再次通过"
        ), authentication(admin));
        approvals.decide(requirementId, new ApprovalService.DecisionRequest(
            ApprovalDecision.APPROVED, "二级通过"
        ), authentication(departmentManager));

        assertThat(requirements.list(authentication(manager)).stream()
            .filter(item -> item.id().equals(requirementId)).findFirst().orElseThrow().status())
            .isEqualTo(RequirementStatus.APPROVED);
        assertThat(approvals.history(requirementId, authentication(manager))).hasSize(3);
    }

    @Test
    @Transactional
    void overdueTasksEscalateEveryTwelveHoursWithoutChangingStatus() {
        UserAccount projectManager = saveUser("escalation-pm", "催办项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount departmentManager = saveUser(
            "escalation-department-manager", "催办部门负责人", UserType.INTERNAL, Role.DEPARTMENT_MANAGER
        );
        UserAccount admin = saveUser("escalation-admin", "催办管理员", UserType.INTERNAL, Role.ADMIN);
        var departmentView = departments.create(new DepartmentService.SaveDepartment(
            "催办测试部门", null, departmentManager.getId()
        ));
        Department department = departmentRepository.findById(departmentView.id()).orElseThrow();
        UserAccount owner = users.save(new UserAccount(
            "escalation-owner", "unused", "催办任务负责人", null,
            UserType.INTERNAL, Set.of(Role.MEMBER), department
        ));
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 12, 0);
        var project = projects.create(new ProjectService.CreateProject(
            "催办测试项目", null, ProjectType.INTERNAL, Priority.HIGH,
            projectManager.getId(), null, null, null
        ), authentication(projectManager));
        var task = projects.createTask(project.id(), new ProjectService.CreateTask(
            "已逾期任务", null, owner.getId(), null, null, Priority.HIGH,
            now.minusDays(2), now.minusHours(25), BigDecimal.TEN
        ), authentication(projectManager));

        assertThat(overdueEscalations.process(now, Instant.parse("2026-07-20T04:00:00Z"))).isEqualTo(1);
        assertThat(overdueEscalations.process(now, Instant.parse("2026-07-20T04:01:00Z"))).isZero();
        assertThat(notifications.inbox(authentication(departmentManager)).items())
            .extracting(NotificationService.NotificationView::escalationLevel).contains(3);

        assertThat(overdueEscalations.process(now.plusHours(12), Instant.parse("2026-07-20T16:00:00Z"))).isEqualTo(1);
        assertThat(notifications.inbox(authentication(admin)).items())
            .extracting(NotificationService.NotificationView::escalationLevel).contains(4);
        assertThat(projects.get(project.id(), authentication(projectManager)).tasks().getFirst().status())
            .isEqualTo(TaskStatus.TODO);
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @Transactional
    void projectRiskKeepsLevelStatusAndTreatmentHistory() {
        UserAccount manager = saveUser("risk-manager", "风险项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount owner = saveUser("risk-owner", "风险负责人", UserType.INTERNAL, Role.MEMBER);
        var project = projects.create(new ProjectService.CreateProject(
            "风险项目", null, ProjectType.INTERNAL, Priority.HIGH, manager.getId(), null, null, null
        ), authentication(manager));
        projects.createTask(project.id(), new ProjectService.CreateTask(
            "风险关联任务", null, owner.getId(), null, null, Priority.HIGH, null, null, BigDecimal.TEN
        ), authentication(manager));
        var risk = riskService.create(project.id(), new RiskService.CreateRisk(
            "关键人员排期冲突", "可能影响里程碑", RiskLevel.CRITICAL, owner.getId(), null, null
        ), authentication(manager));
        risk = riskService.update(risk.id(), new RiskService.UpdateRisk(
            RiskLevel.HIGH, RiskStatus.MITIGATING, "已协调替补人员"
        ), authentication(owner));
        assertThat(risk.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(risk.status()).isEqualTo(RiskStatus.MITIGATING);
        assertThat(risk.updates()).extracting(RiskService.UpdateView::note).containsExactly("已协调替补人员");
    }

    @Test
    @Transactional
    void projectDocumentsKeepImmutableVersionsAndCustomerVisibility() {
        UserAccount manager = saveUser("document-manager", "文档项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount customer = saveUser("document-customer", "文档客户", UserType.CUSTOMER, Role.CUSTOMER);
        var project = projects.create(new ProjectService.CreateProject(
            "文档项目", null, ProjectType.INTERNAL, Priority.MEDIUM,
            manager.getId(), customer.getId(), null, null
        ), authentication(manager));
        var first = documentService.upload(project.id(), null, "交付说明", "初始版", true,
            new MockMultipartFile("file", "delivery.txt", "text/plain", "version-1".getBytes()),
            authentication(manager));
        var second = documentService.upload(project.id(), first.id(), "交付说明", "修订版", true,
            new MockMultipartFile("file", "delivery.txt", "text/plain", "version-2".getBytes()),
            authentication(manager));
        assertThat(second.versions()).extracting(DocumentService.VersionView::versionNo).containsExactly(2, 1);
        assertThat(documentService.list(project.id(), authentication(customer))).hasSize(1);
        assertThat(documentService.download(second.versions().getFirst().id(), authentication(customer)).resource().exists())
            .isTrue();
    }

    @Test
    @Transactional
    void resourceLoadUsesWorkCalendarCapacityAndDetectsConflict() {
        UserAccount manager = saveUser("resource-manager", "资源项目经理", UserType.INTERNAL, Role.PROJECT_MANAGER);
        UserAccount owner = saveUser("resource-owner", "资源成员", UserType.INTERNAL, Role.MEMBER);
        var project = projects.create(new ProjectService.CreateProject(
            "资源项目", null, ProjectType.INTERNAL, Priority.MEDIUM, manager.getId(), null, null, null
        ), authentication(manager));
        LocalDateTime start = LocalDateTime.of(2026, 7, 20, 9, 0);
        projects.createTask(project.id(), new ProjectService.CreateTask(
            "超负荷任务", null, owner.getId(), null, null, Priority.HIGH,
            start, start.plusHours(8), BigDecimal.valueOf(16)
        ), authentication(manager));
        resourceService.saveCapacity(new ResourceService.SaveCapacity(
            owner.getId(), start.toLocalDate(), BigDecimal.valueOf(8), "标准容量"
        ), authentication(manager));

        var report = resourceService.report(start.toLocalDate(), start.toLocalDate(), project.id(), authentication(manager));
        var ownerLoad = report.members().stream().filter(item -> item.userId().equals(owner.getId())).findFirst().orElseThrow();
        assertThat(ownerLoad.capacityHours()).isEqualByComparingTo("8");
        assertThat(ownerLoad.allocatedHours()).isEqualByComparingTo("16");
        assertThat(ownerLoad.loadRate()).isEqualByComparingTo("200.0");
        assertThat(ownerLoad.conflict()).isTrue();
    }

    private UserAccount saveUser(String username, String displayName, UserType type, Role role) {
        return users.save(new UserAccount(username, "unused", displayName, type, Set.of(role)));
    }

    private UsernamePasswordAuthenticationToken authentication(UserAccount user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getUsername(), "n/a", Set.of());
    }
}
