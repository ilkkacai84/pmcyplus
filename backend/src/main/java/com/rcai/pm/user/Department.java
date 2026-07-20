package com.rcai.pm.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private UserAccount manager;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Department() {}

    public Department(String name, Department parent, UserAccount manager) {
        this.name = name;
        this.parent = parent;
        this.manager = manager;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Department getParent() { return parent; }
    public UserAccount getManager() { return manager; }

    public void update(String name, Department parent, UserAccount manager) {
        this.name = name;
        this.parent = parent;
        this.manager = manager;
    }
}
