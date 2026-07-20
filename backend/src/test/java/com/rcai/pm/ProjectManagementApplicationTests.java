package com.rcai.pm;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.project.Priority;
import com.rcai.pm.project.DeliveryStatus;
import com.rcai.pm.project.ProjectService;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.requirement.RequirementService;
import com.rcai.pm.requirement.RequirementSource;
import com.rcai.pm.requirement.RequirementStatus;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.Department;
import com.rcai.pm.user.DepartmentRepository;
import com.rcai.pm.user.DepartmentService;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserController;
import com.rcai.pm.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void contextLoads() {
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
            RequirementSource.WEB, "审批链路需求", "验证状态机", Priority.MEDIUM, null
        ), submitterAuth);
        requirement = requirements.assign(requirement.id(), new RequirementService.AssignRequirement(manager.getId(), null), managerAuth);
        assertThat(requirement.status()).isEqualTo(RequirementStatus.REFINING);

        requirement = requirements.transition(requirement.id(), RequirementStatus.PENDING_APPROVAL, managerAuth);
        assertThat(requirement.status()).isEqualTo(RequirementStatus.PENDING_APPROVAL);
        requirement = requirements.transition(requirement.id(), RequirementStatus.APPROVED, adminAuth);
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
            requirementId, RequirementStatus.REJECTED, submitterAuth
        )).isInstanceOf(com.rcai.pm.common.ApiException.class);

        assertThat(audit.list()).extracting(AuditService.AuditView::action)
            .contains("REQUIREMENT_CREATED", "REQUIREMENT_ASSIGNED", "REQUIREMENT_STATUS_CHANGED",
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

    private UserAccount saveUser(String username, String displayName, UserType type, Role role) {
        return users.save(new UserAccount(username, "unused", displayName, type, Set.of(role)));
    }

    private UsernamePasswordAuthenticationToken authentication(UserAccount user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getUsername(), "n/a", Set.of());
    }
}
