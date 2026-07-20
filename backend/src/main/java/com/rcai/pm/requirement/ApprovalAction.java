package com.rcai.pm.requirement;

import com.rcai.pm.user.UserAccount;
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
@Table(name = "approval_actions")
public class ApprovalAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id")
    private ApprovalInstance instance;
    @Column(name = "step_order", nullable = false)
    private int stepOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private UserAccount actor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;
    @Column(length = 1000)
    private String opinion;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApprovalAction() {}

    public ApprovalAction(ApprovalInstance instance, int stepOrder, UserAccount actor,
                          ApprovalDecision decision, String opinion) {
        this.instance = instance;
        this.stepOrder = stepOrder;
        this.actor = actor;
        this.decision = decision;
        this.opinion = opinion;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public ApprovalInstance getInstance() { return instance; }
    public int getStepOrder() { return stepOrder; }
    public UserAccount getActor() { return actor; }
    public ApprovalDecision getDecision() { return decision; }
    public String getOpinion() { return opinion; }
    public Instant getCreatedAt() { return createdAt; }
}
