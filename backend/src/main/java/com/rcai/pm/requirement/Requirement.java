package com.rcai.pm.requirement;

import com.rcai.pm.project.Priority;
import com.rcai.pm.project.Project;
import com.rcai.pm.project.ProjectType;
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
@Table(name = "requirements")
public class Requirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "requirement_no", nullable = false, unique = true)
    private String requirementNo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementSource source;
    @Column(nullable = false)
    private String title;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id")
    private UserAccount submitter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private UserAccount customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserAccount assignee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementStatus status = RequirementStatus.UNASSIGNED;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    private Long mergedIntoId;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Requirement() {}

    public Requirement(String requirementNo, RequirementSource source, String title, String description,
                       UserAccount submitter, UserAccount customer, Priority priority, ProjectType projectType) {
        this.requirementNo = requirementNo;
        this.source = source;
        this.title = title;
        this.description = description;
        this.submitter = submitter;
        this.customer = customer;
        this.priority = priority;
        this.projectType = projectType;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public String getRequirementNo() { return requirementNo; }
    public RequirementSource getSource() { return source; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UserAccount getSubmitter() { return submitter; }
    public UserAccount getCustomer() { return customer; }
    public UserAccount getAssignee() { return assignee; }
    public Project getProject() { return project; }
    public ProjectType getProjectType() { return projectType; }
    public RequirementStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getMergedIntoId() { return mergedIntoId; }

    public void markMerged(Long targetId) {
        this.mergedIntoId = targetId;
        this.status = RequirementStatus.MERGED;
        this.updatedAt = Instant.now();
    }

    public void assign(UserAccount assignee, Project project) {
        this.assignee = assignee;
        this.project = project;
        if (project != null) this.projectType = project.getProjectType();
        this.status = RequirementStatus.REFINING;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(RequirementStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void linkProject(Project project) {
        this.project = project;
        this.updatedAt = Instant.now();
    }
}
