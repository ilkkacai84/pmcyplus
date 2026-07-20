package com.rcai.pm;

import com.rcai.pm.project.Priority;
import com.rcai.pm.project.DeliveryStatus;
import com.rcai.pm.project.ProjectService;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.requirement.RequirementService;
import com.rcai.pm.requirement.RequirementSource;
import com.rcai.pm.requirement.RequirementStatus;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    }

    private UserAccount saveUser(String username, String displayName, UserType type, Role role) {
        return users.save(new UserAccount(username, "unused", displayName, type, Set.of(role)));
    }

    private UsernamePasswordAuthenticationToken authentication(UserAccount user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getUsername(), "n/a", Set.of());
    }
}
