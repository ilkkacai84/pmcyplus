package com.rcai.pm.risk;

import com.rcai.pm.project.*;
import com.rcai.pm.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "risks")
public class Risk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "milestone_id") private Milestone milestone;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id") private TaskItem task;
    @Column(nullable = false, length = 240) private String title;
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false) private RiskLevel riskLevel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RiskStatus status = RiskStatus.OPEN;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") private UserAccount owner;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Risk() {}
    public Risk(Project project, Milestone milestone, TaskItem task, String title, String description, RiskLevel level, UserAccount owner) {
        this.project=project; this.milestone=milestone; this.task=task; this.title=title; this.description=description;
        this.riskLevel=level; this.owner=owner; this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public void update(RiskLevel level, RiskStatus status) { this.riskLevel=level; this.status=status; this.updatedAt=Instant.now(); }
    public Long getId(){return id;} public Project getProject(){return project;} public Milestone getMilestone(){return milestone;}
    public TaskItem getTask(){return task;} public String getTitle(){return title;} public String getDescription(){return description;}
    public RiskLevel getRiskLevel(){return riskLevel;} public RiskStatus getStatus(){return status;} public UserAccount getOwner(){return owner;}
    public Instant getCreatedAt(){return createdAt;}
}
