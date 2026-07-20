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
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private UserAccount manager;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private UserAccount customer;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    @Column(nullable = false)
    private BigDecimal budget = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal laborCost = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal otherCost = BigDecimal.ZERO;
    private Long mergedIntoId;
    @Version
    private long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Project() {}

    public Project(String code, String name, String description, ProjectType projectType, Priority priority,
                   UserAccount manager, UserAccount customer, LocalDateTime plannedStartAt, LocalDateTime plannedEndAt) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.projectType = projectType;
        this.priority = priority;
        this.manager = manager;
        this.customer = customer;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.status = ProjectStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectType getProjectType() { return projectType; }
    public ProjectStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public UserAccount getManager() { return manager; }
    public UserAccount getCustomer() { return customer; }
    public LocalDateTime getPlannedStartAt() { return plannedStartAt; }
    public LocalDateTime getPlannedEndAt() { return plannedEndAt; }
    public LocalDateTime getActualStartAt() { return actualStartAt; }
    public LocalDateTime getActualEndAt() { return actualEndAt; }
    public BigDecimal getBudget() { return budget; }
    public BigDecimal getLaborCost() { return laborCost; }
    public BigDecimal getOtherCost() { return otherCost; }

    public void changeStatus(ProjectStatus status) {
        this.status = status;
        if (status == ProjectStatus.ACTIVE && actualStartAt == null) actualStartAt = LocalDateTime.now();
        if (status == ProjectStatus.COMPLETED) actualEndAt = LocalDateTime.now();
        this.updatedAt = Instant.now();
    }

    public void updateFinancials(BigDecimal budget, BigDecimal laborCost, BigDecimal otherCost) {
        this.budget = budget; this.laborCost = laborCost; this.otherCost = otherCost; this.updatedAt = Instant.now();
    }
}
