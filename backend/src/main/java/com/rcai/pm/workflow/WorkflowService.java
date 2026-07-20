package com.rcai.pm.workflow;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.ProjectType;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WorkflowService {
    private final WorkflowTemplateRepository templates;
    private final WorkflowTransitionRepository transitions;
    private final AuditService audit;
    private final WorkflowApprovalStepRepository approvalSteps;

    public WorkflowService(WorkflowTemplateRepository templates, WorkflowTransitionRepository transitions,
                           AuditService audit, WorkflowApprovalStepRepository approvalSteps) {
        this.templates = templates;
        this.transitions = transitions;
        this.audit = audit;
        this.approvalSteps = approvalSteps;
    }

    public List<TemplateView> list() {
        return templates.findAllByOrderByProjectTypeAsc().stream().map(template -> new TemplateView(
            template.getId(), template.getName(), template.getProjectType(), template.isActive(),
            transitions.findByTemplateIdOrderByObjectTypeAscFromStatusAscToStatusAsc(template.getId()).stream()
                .map(TransitionView::from).toList(),
            approvalSteps.findByTemplateIdOrderByStepOrderAsc(template.getId()).stream()
                .map(ApprovalStepView::from).toList()
        )).toList();
    }

    public WorkflowTransition requireTransition(ProjectType projectType, WorkflowObjectType objectType,
                                                String fromStatus, String toStatus, UserAccount actor, String reason) {
        WorkflowTransition transition = transitions
            .findByTemplateProjectTypeAndObjectTypeAndFromStatusAndToStatus(
                projectType, objectType, fromStatus, toStatus
            ).filter(WorkflowTransition::isEnabled)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "当前项目流程不允许该状态变更"));
        boolean allowed = actor.getRoles().stream().anyMatch(transition.getAllowedRoles()::contains);
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "当前角色不能执行该流程动作");
        if (transition.isRequiresReason() && (reason == null || reason.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该流程动作必须填写原因");
        }
        return transition;
    }

    @Transactional
    public TransitionView update(Long id, ConfigureTransition request, Authentication authentication) {
        WorkflowTransition transition = transitions.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "流程迁移不存在"));
        transition.configure(request.allowedRoles(), request.enabled(), request.requiresReason(), request.notificationEvent());
        audit.log(authentication, "WORKFLOW_TRANSITION_CONFIGURED", "WORKFLOW_TRANSITION", id, Map.of(
            "enabled", request.enabled(), "roles", request.allowedRoles().toString()
        ));
        return TransitionView.from(transition);
    }

    @Transactional
    public ApprovalStepView addApprovalStep(Long templateId, SaveApprovalStep request, Authentication authentication) {
        WorkflowTemplate template = templates.findById(templateId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "流程模板不存在"));
        int order = approvalSteps.findByTemplateIdOrderByStepOrderAsc(templateId).stream()
            .mapToInt(WorkflowApprovalStep::getStepOrder).max().orElse(0) + 1;
        WorkflowApprovalStep step = approvalSteps.save(new WorkflowApprovalStep(
            template, order, request.name().trim(), request.approverRole()
        ));
        audit.log(authentication, "APPROVAL_STEP_CREATED", "WORKFLOW_TEMPLATE", templateId,
            Map.of("step", order, "role", request.approverRole().name()));
        return ApprovalStepView.from(step);
    }

    @Transactional
    public ApprovalStepView updateApprovalStep(Long id, SaveApprovalStep request, Authentication authentication) {
        WorkflowApprovalStep step = approvalSteps.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "审批步骤不存在"));
        step.update(request.name().trim(), request.approverRole(), request.active());
        audit.log(authentication, "APPROVAL_STEP_UPDATED", "WORKFLOW_APPROVAL_STEP", id,
            Map.of("role", request.approverRole().name(), "active", request.active()));
        return ApprovalStepView.from(step);
    }

    public record ConfigureTransition(@NotEmpty Set<Role> allowedRoles, boolean enabled, boolean requiresReason,
                                      String notificationEvent) {}
    public record TemplateView(Long id, String name, ProjectType projectType, boolean active,
                               List<TransitionView> transitions, List<ApprovalStepView> approvalSteps) {}
    public record TransitionView(Long id, WorkflowObjectType objectType, String fromStatus, String toStatus,
                                 Set<Role> allowedRoles, boolean enabled, boolean requiresReason,
                                 String notificationEvent) {
        static TransitionView from(WorkflowTransition transition) {
            return new TransitionView(transition.getId(), transition.getObjectType(), transition.getFromStatus(),
                transition.getToStatus(), transition.getAllowedRoles(), transition.isEnabled(),
                transition.isRequiresReason(), transition.getNotificationEvent());
        }
    }
    public record SaveApprovalStep(@NotBlank String name, @NotNull Role approverRole, boolean active) {}
    public record ApprovalStepView(Long id, int stepOrder, String name, Role approverRole, boolean active) {
        static ApprovalStepView from(WorkflowApprovalStep step) {
            return new ApprovalStepView(step.getId(), step.getStepOrder(), step.getName(),
                step.getApproverRole(), step.isActive());
        }
    }
}
