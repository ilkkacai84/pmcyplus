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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "delivery_versions", uniqueConstraints = @UniqueConstraint(name = "uk_delivery_task_version", columnNames = {"task_id", "version_no"}))
public class DeliveryVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private TaskItem task;
    @Column(name = "version_no", nullable = false)
    private int versionNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by")
    private UserAccount submittedBy;
    @Column(name = "submission_note", length = 1000)
    private String submissionNote;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;
    @Column(name = "review_opinion", length = 1000)
    private String reviewOpinion;
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected DeliveryVersion() {}

    public DeliveryVersion(TaskItem task, int versionNo, UserAccount submittedBy, String submissionNote) {
        this.task = task;
        this.versionNo = versionNo;
        this.submittedBy = submittedBy;
        this.submissionNote = submissionNote;
        this.submittedAt = Instant.now();
    }

    public void review(DeliveryStatus decision, UserAccount reviewer, String opinion) {
        if (decision == DeliveryStatus.PENDING) throw new IllegalArgumentException("验收结果不能为待处理");
        this.status = decision;
        this.reviewedBy = reviewer;
        this.reviewOpinion = opinion;
        this.reviewedAt = Instant.now();
    }

    public Long getId() { return id; }
    public TaskItem getTask() { return task; }
    public int getVersionNo() { return versionNo; }
    public UserAccount getSubmittedBy() { return submittedBy; }
    public String getSubmissionNote() { return submissionNote; }
    public DeliveryStatus getStatus() { return status; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public String getReviewOpinion() { return reviewOpinion; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
