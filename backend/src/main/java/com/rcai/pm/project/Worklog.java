package com.rcai.pm.project;

import com.rcai.pm.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "worklogs")
public class Worklog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private TaskItem task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(nullable = false)
    private BigDecimal hours;
    private String note;
    @Column(nullable = false)
    private LocalDate workedOn;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Worklog() {}

    public Worklog(TaskItem task, UserAccount user, BigDecimal hours, String note, LocalDate workedOn) {
        this.task = task;
        this.user = user;
        this.hours = hours;
        this.note = note;
        this.workedOn = workedOn;
        this.createdAt = Instant.now();
    }
    public UserAccount getUser() { return user; }
    public TaskItem getTask() { return task; }
    public BigDecimal getHours() { return hours; }
    public LocalDate getWorkedOn() { return workedOn; }
}
