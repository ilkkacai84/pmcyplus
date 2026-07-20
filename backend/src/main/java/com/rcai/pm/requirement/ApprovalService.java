package com.rcai.pm.requirement;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.notification.NotificationService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.workflow.WorkflowApprovalStep;
import com.rcai.pm.workflow.WorkflowApprovalStepRepository;
import com.rcai.pm.workflow.WorkflowTemplate;
import com.rcai.pm.workflow.WorkflowTemplateRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ApprovalService {
    private final ApprovalInstanceRepository instances;
    private final ApprovalActionRepository actions;
    private final RequirementRepository requirements;
    private final UserAccountRepository users;
    private final WorkflowTemplateRepository templates;
    private final WorkflowApprovalStepRepository steps;
    private final AuditService audit;
    private final NotificationService notifications;

    public ApprovalService(ApprovalInstanceRepository instances, ApprovalActionRepository actions,
                           RequirementRepository requirements, UserAccountRepository users,
                           WorkflowTemplateRepository templates, WorkflowApprovalStepRepository steps,
                           AuditService audit, NotificationService notifications) {
        this.instances = instances;
        this.actions = actions;
        this.requirements = requirements;
        this.users = users;
        this.templates = templates;
        this.steps = steps;
        this.audit = audit;
        this.notifications = notifications;
    }

    @Transactional
    public ApprovalView submit(Long requirementId, Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirement(requirementId);
        boolean owner = requirement.getAssignee() != null && requirement.getAssignee().getId().equals(actor.getId());
        if (!owner && !actor.getRoles().contains(Role.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有需求负责人可以提交审批");
        }
        if (!List.of(RequirementStatus.REFINING, RequirementStatus.REJECTED).contains(requirement.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "当前需求不能提交审批");
        }
        List<WorkflowApprovalStep> approvalSteps = activeSteps(requirement);
        if (approvalSteps.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "当前项目类型未配置审批步骤");
        if (latest(requirementId).map(item -> item.getStatus() == ApprovalStatus.PENDING).orElse(false)) {
            throw new ApiException(HttpStatus.CONFLICT, "需求已有进行中的审批");
        }
        WorkflowTemplate template = templates.findByProjectTypeAndActiveTrue(requirement.getProjectType())
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "审批流程模板未启用"));
        ApprovalInstance instance = instances.save(new ApprovalInstance(requirement, template, actor));
        requirement.changeStatus(RequirementStatus.PENDING_APPROVAL);
        audit.log(actor, "APPROVAL_SUBMITTED", "REQUIREMENT", requirementId,
            Map.of("instanceId", instance.getId(), "projectType", requirement.getProjectType().name()));
        notifications.notify(users.findEnabledByRole(approvalSteps.getFirst().getApproverRole()), "APPROVAL_PENDING",
            "有新的需求待审批", requirement.getTitle(), "REQUIREMENT", requirementId, 0);
        return view(instance, approvalSteps);
    }

    @Transactional
    public ApprovalView decide(Long requirementId, DecisionRequest request, Authentication authentication) {
        if (request.decision() == ApprovalDecision.WITHDRAWN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "撤回请使用撤回操作");
        }
        UserAccount actor = current(authentication);
        Requirement requirement = requirement(requirementId);
        ApprovalInstance instance = pending(requirementId);
        List<WorkflowApprovalStep> approvalSteps = activeSteps(requirement);
        WorkflowApprovalStep currentStep = approvalSteps.stream()
            .filter(step -> step.getStepOrder() == instance.getCurrentStep()).findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "当前审批步骤配置不存在"));
        if (!actor.getRoles().contains(Role.ADMIN) && !actor.getRoles().contains(currentStep.getApproverRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "当前角色不能审批此步骤");
        }
        if (request.decision() == ApprovalDecision.REJECTED
            && (request.opinion() == null || request.opinion().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "驳回时必须填写审批意见");
        }
        actions.save(new ApprovalAction(instance, currentStep.getStepOrder(), actor,
            request.decision(), request.opinion()));
        if (request.decision() == ApprovalDecision.REJECTED) {
            instance.complete(ApprovalStatus.REJECTED);
            requirement.changeStatus(RequirementStatus.REJECTED);
        } else if (instance.getCurrentStep() == approvalSteps.getLast().getStepOrder()) {
            instance.complete(ApprovalStatus.APPROVED);
            requirement.changeStatus(RequirementStatus.APPROVED);
        } else {
            instance.advance();
        }
        audit.log(actor, "APPROVAL_DECIDED", "REQUIREMENT", requirementId, Map.of(
            "instanceId", instance.getId(), "step", currentStep.getStepOrder(), "decision", request.decision().name()
        ));
        if (instance.getStatus() == ApprovalStatus.PENDING) {
            WorkflowApprovalStep next = approvalSteps.stream()
                .filter(step -> step.getStepOrder() == instance.getCurrentStep()).findFirst().orElseThrow();
            notifications.notify(users.findEnabledByRole(next.getApproverRole()), "APPROVAL_PENDING",
                "需求进入下一审批步骤", requirement.getTitle(), "REQUIREMENT", requirementId, 0);
        } else {
            notifications.notify(List.of(requirement.getAssignee(), requirement.getSubmitter()), "APPROVAL_COMPLETED",
                "需求审批结果：" + instance.getStatus(), requirement.getTitle(), "REQUIREMENT", requirementId, 0);
        }
        return view(instance, approvalSteps);
    }

    @Transactional
    public ApprovalView withdraw(Long requirementId, OpinionRequest request, Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirement(requirementId);
        ApprovalInstance instance = pending(requirementId);
        boolean allowed = actor.getRoles().contains(Role.ADMIN)
            || instance.getSubmittedBy().getId().equals(actor.getId())
            || (requirement.getAssignee() != null && requirement.getAssignee().getId().equals(actor.getId()));
        if (!allowed) throw new ApiException(HttpStatus.FORBIDDEN, "当前用户不能撤回该审批");
        actions.save(new ApprovalAction(instance, instance.getCurrentStep(), actor,
            ApprovalDecision.WITHDRAWN, request.opinion()));
        instance.complete(ApprovalStatus.WITHDRAWN);
        requirement.changeStatus(RequirementStatus.REFINING);
        audit.log(actor, "APPROVAL_WITHDRAWN", "REQUIREMENT", requirementId,
            Map.of("instanceId", instance.getId()));
        return view(instance, activeSteps(requirement));
    }

    public List<ApprovalView> history(Long requirementId, Authentication authentication) {
        UserAccount actor = current(authentication);
        Requirement requirement = requirement(requirementId);
        boolean accessible = actor.getRoles().contains(Role.ADMIN)
            || requirement.getSubmitter().getId().equals(actor.getId())
            || (requirement.getCustomer() != null && requirement.getCustomer().getId().equals(actor.getId()))
            || (requirement.getAssignee() != null && requirement.getAssignee().getId().equals(actor.getId()));
        if (!accessible) throw new ApiException(HttpStatus.FORBIDDEN, "无权查看审批记录");
        List<WorkflowApprovalStep> approvalSteps = activeSteps(requirement);
        return instances.findByRequirementIdOrderByCreatedAtDesc(requirementId).stream()
            .map(instance -> view(instance, approvalSteps)).toList();
    }

    private ApprovalView view(ApprovalInstance instance, List<WorkflowApprovalStep> approvalSteps) {
        String stepName = approvalSteps.stream().filter(step -> step.getStepOrder() == instance.getCurrentStep())
            .map(WorkflowApprovalStep::getName).findFirst().orElse(null);
        List<ActionView> actionViews = actions.findByInstanceIdOrderByCreatedAtAsc(instance.getId()).stream()
            .map(ActionView::from).toList();
        return new ApprovalView(instance.getId(), instance.getStatus(), instance.getCurrentStep(), stepName,
            instance.getSubmittedBy().getDisplayName(), instance.getCreatedAt(), instance.getCompletedAt(), actionViews);
    }

    private List<WorkflowApprovalStep> activeSteps(Requirement requirement) {
        return steps.findByTemplateProjectTypeAndActiveTrueOrderByStepOrderAsc(requirement.getProjectType());
    }

    private java.util.Optional<ApprovalInstance> latest(Long requirementId) {
        return instances.findFirstByRequirementIdOrderByCreatedAtDesc(requirementId);
    }

    private ApprovalInstance pending(Long requirementId) {
        return latest(requirementId).filter(item -> item.getStatus() == ApprovalStatus.PENDING)
            .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "没有进行中的审批"));
    }

    private Requirement requirement(Long id) {
        return requirements.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "需求不存在"));
    }

    private UserAccount current(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    public record DecisionRequest(@NotNull ApprovalDecision decision, String opinion) {}
    public record OpinionRequest(String opinion) {}
    public record ApprovalView(Long id, ApprovalStatus status, int currentStep, String currentStepName,
                               String submitterName, Instant createdAt, Instant completedAt, List<ActionView> actions) {}
    public record ActionView(Long id, int stepOrder, String actorName, ApprovalDecision decision,
                             String opinion, Instant createdAt) {
        static ActionView from(ApprovalAction action) {
            return new ActionView(action.getId(), action.getStepOrder(), action.getActor().getDisplayName(),
                action.getDecision(), action.getOpinion(), action.getCreatedAt());
        }
    }
}
