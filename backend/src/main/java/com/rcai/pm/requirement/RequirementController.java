package com.rcai.pm.requirement;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {
    private final RequirementService service;
    private final ApprovalService approvals;

    public RequirementController(RequirementService service, ApprovalService approvals) {
        this.service = service;
        this.approvals = approvals;
    }

    @GetMapping
    public List<RequirementService.RequirementView> list(Authentication authentication) {
        return service.list(authentication);
    }

    @PostMapping
    public RequirementService.RequirementView create(@Valid @RequestBody RequirementService.CreateRequirement request, Authentication authentication) {
        return service.create(request, authentication);
    }

    @PostMapping("/{id}/assign")
    public RequirementService.RequirementView assign(@PathVariable Long id,
                                                       @Valid @RequestBody RequirementService.AssignRequirement request,
                                                       Authentication authentication) {
        return service.assign(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    public RequirementService.RequirementView transition(@PathVariable Long id, @RequestParam RequirementStatus status,
                                                           @RequestParam(required = false) String opinion,
                                                           Authentication authentication) {
        return service.transition(id, status, opinion, authentication);
    }

    @PostMapping("/{id}/approval/submit")
    public ApprovalService.ApprovalView submitApproval(@PathVariable Long id, Authentication authentication) {
        return approvals.submit(id, authentication);
    }

    @PostMapping("/{id}/approval/decision")
    public ApprovalService.ApprovalView decide(@PathVariable Long id,
                                               @Valid @RequestBody ApprovalService.DecisionRequest request,
                                               Authentication authentication) {
        return approvals.decide(id, request, authentication);
    }

    @PostMapping("/{id}/approval/withdraw")
    public ApprovalService.ApprovalView withdraw(@PathVariable Long id,
                                                 @RequestBody ApprovalService.OpinionRequest request,
                                                 Authentication authentication) {
        return approvals.withdraw(id, request, authentication);
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalService.ApprovalView> approvalHistory(@PathVariable Long id, Authentication authentication) {
        return approvals.history(id, authentication);
    }

    @PostMapping("/{id}/project")
    public com.rcai.pm.project.ProjectService.ProjectSummary createProject(
        @PathVariable Long id,
        @Valid @RequestBody RequirementService.CreateProjectFromRequirement request,
        Authentication authentication
    ) {
        return service.createProject(id, request, authentication);
    }
}
