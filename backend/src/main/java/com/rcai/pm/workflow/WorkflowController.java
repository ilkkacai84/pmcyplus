package com.rcai.pm.workflow;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public List<WorkflowService.TemplateView> list() {
        return service.list();
    }

    @PutMapping("/transitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkflowService.TransitionView update(@PathVariable Long id,
                                                 @Valid @RequestBody WorkflowService.ConfigureTransition request,
                                                 Authentication authentication) {
        return service.update(id, request, authentication);
    }

    @PostMapping("/{templateId}/approval-steps")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkflowService.ApprovalStepView addApprovalStep(
        @PathVariable Long templateId, @Valid @RequestBody WorkflowService.SaveApprovalStep request,
        Authentication authentication
    ) {
        return service.addApprovalStep(templateId, request, authentication);
    }

    @PutMapping("/approval-steps/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkflowService.ApprovalStepView updateApprovalStep(
        @PathVariable Long id, @Valid @RequestBody WorkflowService.SaveApprovalStep request,
        Authentication authentication
    ) {
        return service.updateApprovalStep(id, request, authentication);
    }
}
