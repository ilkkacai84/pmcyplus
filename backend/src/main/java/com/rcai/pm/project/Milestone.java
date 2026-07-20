package com.rcai.pm.project;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "milestones")
public class Milestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;
    @Column(nullable = false)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;
    private LocalDateTime plannedAt;
    private LocalDateTime actualAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.DRAFT;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Milestone() {}

    public Milestone(Project project, String name, UserAccount owner, LocalDateTime plannedAt) {
        this.project = project;
        this.name = name;
        this.owner = owner;
        this.plannedAt = plannedAt;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getName() { return name; }
    public UserAccount getOwner() { return owner; }
    public LocalDateTime getPlannedAt() { return plannedAt; }
    public LocalDateTime getActualAt() { return actualAt; }
    public ProjectStatus getStatus() { return status; }
}
