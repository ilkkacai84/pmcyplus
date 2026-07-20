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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_items")
public class TaskItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private TaskItem parentTask;
    @Column(nullable = false, length = 240)
    private String title;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    @Column(nullable = false)
    private BigDecimal estimatedHours = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal actualHours = BigDecimal.ZERO;
    private Long mergedIntoId;
    @Version
    private long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected TaskItem() {}

    public TaskItem(Project project, Milestone milestone, TaskItem parentTask, String title, String description,
                    UserAccount owner, Priority priority, LocalDateTime plannedStartAt,
                    LocalDateTime plannedEndAt, BigDecimal estimatedHours) {
        this.project = project;
        this.milestone = milestone;
        this.parentTask = parentTask;
        this.title = title;
        this.description = description;
        this.owner = owner;
        this.priority = priority;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.estimatedHours = estimatedHours == null ? BigDecimal.ZERO : estimatedHours;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public Milestone getMilestone() { return milestone; }
    public TaskItem getParentTask() { return parentTask; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public UserAccount getOwner() { return owner; }
    public TaskStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public LocalDateTime getPlannedStartAt() { return plannedStartAt; }
    public LocalDateTime getPlannedEndAt() { return plannedEndAt; }
    public LocalDateTime getActualStartAt() { return actualStartAt; }
    public LocalDateTime getActualEndAt() { return actualEndAt; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public BigDecimal getActualHours() { return actualHours; }

    public void changeStatus(TaskStatus newStatus) {
        this.status = newStatus;
        if (newStatus == TaskStatus.IN_PROGRESS && actualStartAt == null) actualStartAt = LocalDateTime.now();
        if (newStatus == TaskStatus.COMPLETED) actualEndAt = LocalDateTime.now();
        this.updatedAt = Instant.now();
    }

    public void addActualHours(BigDecimal hours) {
        this.actualHours = this.actualHours.add(hours);
        this.updatedAt = Instant.now();
    }
}
