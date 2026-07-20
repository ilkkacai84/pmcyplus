package com.rcai.pm.workflow;

import com.rcai.pm.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "workflow_approval_steps", uniqueConstraints = @UniqueConstraint(
    name = "uk_workflow_approval_step", columnNames = {"template_id", "step_order"}
))
public class WorkflowApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private WorkflowTemplate template;
    @Column(name = "step_order", nullable = false)
    private int stepOrder;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false)
    private Role approverRole;
    @Column(nullable = false)
    private boolean active = true;

    protected WorkflowApprovalStep() {}

    public WorkflowApprovalStep(WorkflowTemplate template, int stepOrder, String name, Role approverRole) {
        this.template = template;
        this.stepOrder = stepOrder;
        this.name = name;
        this.approverRole = approverRole;
    }

    public void update(String name, Role approverRole, boolean active) {
        this.name = name;
        this.approverRole = approverRole;
        this.active = active;
    }

    public Long getId() { return id; }
    public WorkflowTemplate getTemplate() { return template; }
    public int getStepOrder() { return stepOrder; }
    public String getName() { return name; }
    public Role getApproverRole() { return approverRole; }
    public boolean isActive() { return active; }
}
