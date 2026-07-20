package com.rcai.pm.notification;

import com.rcai.pm.project.TaskItem;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_escalations")
public class TaskEscalation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id", unique = true) private TaskItem task;
    @Column(name = "escalation_level", nullable = false) private int escalationLevel;
    @Column(name = "last_notified_at") private Instant lastNotifiedAt;
    protected TaskEscalation() {}
    public TaskEscalation(TaskItem task) { this.task = task; }
    public void advance(int level, Instant time) { escalationLevel = level; lastNotifiedAt = time; }
    public Long getId() { return id; } public TaskItem getTask() { return task; }
    public int getEscalationLevel() { return escalationLevel; } public Instant getLastNotifiedAt() { return lastNotifiedAt; }
}
