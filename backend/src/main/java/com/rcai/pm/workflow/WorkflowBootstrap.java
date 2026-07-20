package com.rcai.pm.workflow;

import com.rcai.pm.project.ProjectType;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.user.Role;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
public class WorkflowBootstrap implements ApplicationRunner {
    private final WorkflowTemplateRepository templates;
    private final WorkflowTransitionRepository transitions;
    private final WorkflowApprovalStepRepository approvalSteps;

    public WorkflowBootstrap(WorkflowTemplateRepository templates, WorkflowTransitionRepository transitions,
                             WorkflowApprovalStepRepository approvalSteps) {
        this.templates = templates;
        this.transitions = transitions;
        this.approvalSteps = approvalSteps;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ProjectType type : ProjectType.values()) {
            WorkflowTemplate template = templates.findByProjectTypeAndActiveTrue(type)
                .orElseGet(() -> templates.save(new WorkflowTemplate(templateName(type), type)));
            if (approvalSteps.countByTemplateId(template.getId()) == 0) {
                approvalSteps.save(new WorkflowApprovalStep(template, 1, "管理员审批", Role.ADMIN));
            }
            for (DefaultTransition definition : defaults()) {
                if (transitions.findByTemplateProjectTypeAndObjectTypeAndFromStatusAndToStatus(
                    type, WorkflowObjectType.TASK, definition.from().name(), definition.to().name()
                ).isEmpty()) {
                    transitions.save(new WorkflowTransition(template, WorkflowObjectType.TASK,
                        definition.from().name(), definition.to().name(), definition.roles(), definition.event()));
                }
            }
        }
    }

    private String templateName(ProjectType type) {
        return type == ProjectType.INTERNAL ? "内部项目默认流程" : "临时任务默认流程";
    }

    private List<DefaultTransition> defaults() {
        Set<Role> members = Set.of(Role.ADMIN, Role.PROJECT_MANAGER, Role.MEMBER);
        Set<Role> managers = Set.of(Role.ADMIN, Role.PROJECT_MANAGER);
        Set<Role> customers = Set.of(Role.ADMIN, Role.CUSTOMER);
        return List.of(
            new DefaultTransition(TaskStatus.TODO, TaskStatus.IN_PROGRESS, members, "TASK_STARTED"),
            new DefaultTransition(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, members, "TASK_BLOCKED"),
            new DefaultTransition(TaskStatus.BLOCKED, TaskStatus.IN_PROGRESS, members, "TASK_RESUMED"),
            new DefaultTransition(TaskStatus.TODO, TaskStatus.CANCELLED, managers, "TASK_CANCELLED"),
            new DefaultTransition(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, managers, "TASK_CANCELLED"),
            new DefaultTransition(TaskStatus.BLOCKED, TaskStatus.CANCELLED, managers, "TASK_CANCELLED"),
            new DefaultTransition(TaskStatus.CANCELLED, TaskStatus.TODO, managers, "TASK_REOPENED"),
            new DefaultTransition(TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS, managers, "TASK_REOPENED"),
            new DefaultTransition(TaskStatus.IN_PROGRESS, TaskStatus.PENDING_ACCEPTANCE, members, "DELIVERY_SUBMITTED"),
            new DefaultTransition(TaskStatus.BLOCKED, TaskStatus.PENDING_ACCEPTANCE, members, "DELIVERY_SUBMITTED"),
            new DefaultTransition(TaskStatus.PENDING_ACCEPTANCE, TaskStatus.COMPLETED, customers, "DELIVERY_ACCEPTED"),
            new DefaultTransition(TaskStatus.PENDING_ACCEPTANCE, TaskStatus.IN_PROGRESS, customers, "DELIVERY_RETURNED")
        );
    }

    private record DefaultTransition(TaskStatus from, TaskStatus to, Set<Role> roles, String event) {}
}
