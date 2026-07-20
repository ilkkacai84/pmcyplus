package com.rcai.pm.workflow;

import com.rcai.pm.project.ProjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "workflow_templates")
public class WorkflowTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, unique = true)
    private ProjectType projectType;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowTemplate() {}

    public WorkflowTemplate(String name, ProjectType projectType) {
        this.name = name;
        this.projectType = projectType;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProjectType getProjectType() { return projectType; }
    public boolean isActive() { return active; }
}
