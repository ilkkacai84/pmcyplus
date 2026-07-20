package com.rcai.pm.requirement;

import com.rcai.pm.user.UserAccount;
import com.rcai.pm.workflow.WorkflowTemplate;
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

import java.time.Instant;

@Entity
@Table(name = "approval_instances")
public class ApprovalInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private WorkflowTemplate template;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;
    @Column(name = "current_step", nullable = false)
    private int currentStep = 1;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by")
    private UserAccount submittedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected ApprovalInstance() {}

    public ApprovalInstance(Requirement requirement, WorkflowTemplate template, UserAccount submittedBy) {
        this.requirement = requirement;
        this.template = template;
        this.submittedBy = submittedBy;
        this.createdAt = Instant.now();
    }

    public void advance() { this.currentStep++; }
    public void complete(ApprovalStatus status) { this.status = status; this.completedAt = Instant.now(); }

    public Long getId() { return id; }
    public Requirement getRequirement() { return requirement; }
    public WorkflowTemplate getTemplate() { return template; }
    public ApprovalStatus getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
    public UserAccount getSubmittedBy() { return submittedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
