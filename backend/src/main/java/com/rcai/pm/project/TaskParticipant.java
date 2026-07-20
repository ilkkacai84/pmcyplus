package com.rcai.pm.project;

import com.rcai.pm.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "task_participants")
@IdClass(TaskParticipantId.class)
public class TaskParticipant {
    @Id
    @Column(name = "task_id")
    private Long taskId;
    @Id
    @Column(name = "user_id")
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private TaskItem task;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccount user;
    @Column(name = "participant_type", nullable = false, length = 30)
    private String participantType;
    @Column(name = "planned_hours", nullable = false)
    private BigDecimal plannedHours = BigDecimal.ZERO;

    protected TaskParticipant() {}

    public TaskParticipant(Long taskId, Long userId, String participantType, BigDecimal plannedHours) {
        this.taskId = taskId;
        this.userId = userId;
        this.participantType = participantType;
        this.plannedHours = plannedHours == null ? BigDecimal.ZERO : plannedHours;
    }

    public Long getTaskId() { return taskId; }
    public Long getUserId() { return userId; }
    public UserAccount getUser() { return user; }
    public String getParticipantType() { return participantType; }
    public BigDecimal getPlannedHours() { return plannedHours; }
}
