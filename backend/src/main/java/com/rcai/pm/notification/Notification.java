package com.rcai.pm.notification;

import com.rcai.pm.user.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recipient_id") private UserAccount recipient;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationStatus status;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(nullable = false, length = 240) private String title;
    @Column(length = 1000) private String content;
    @Column(name = "object_type", length = 40) private String objectType;
    @Column(name = "object_id") private Long objectId;
    @Column(name = "escalation_level", nullable = false) private int escalationLevel;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "last_attempt_at") private Instant lastAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;

    protected Notification() {}
    public Notification(UserAccount recipient, NotificationChannel channel, String eventType, String title,
                        String content, String objectType, Long objectId, int escalationLevel) {
        this.recipient = recipient; this.channel = channel; this.eventType = eventType; this.title = title;
        this.content = content; this.objectType = objectType; this.objectId = objectId;
        this.escalationLevel = escalationLevel; this.createdAt = Instant.now();
        this.status = channel == NotificationChannel.SITE ? NotificationStatus.SENT : NotificationStatus.PENDING;
    }
    public void markRead() { if (channel == NotificationChannel.SITE) { status = NotificationStatus.READ; readAt = Instant.now(); } }
    public void markSent() { status = NotificationStatus.SENT; sentAt = Instant.now(); lastError = null; }
    public void markAttemptFailed(Exception exception) {
        attemptCount++;
        lastAttemptAt = Instant.now();
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        lastError = message.length() > 1000 ? message.substring(0, 1000) : message;
        status = attemptCount >= 3 ? NotificationStatus.FAILED : NotificationStatus.PENDING;
    }
    public Long getId() { return id; } public UserAccount getRecipient() { return recipient; }
    public NotificationChannel getChannel() { return channel; } public NotificationStatus getStatus() { return status; }
    public String getEventType() { return eventType; } public String getTitle() { return title; }
    public String getContent() { return content; } public String getObjectType() { return objectType; }
    public Long getObjectId() { return objectId; } public int getEscalationLevel() { return escalationLevel; }
    public Instant getCreatedAt() { return createdAt; } public Instant getReadAt() { return readAt; }
    public int getAttemptCount() { return attemptCount; } public String getLastError() { return lastError; }
    public Instant getLastAttemptAt() { return lastAttemptAt; } public Instant getSentAt() { return sentAt; }
}
