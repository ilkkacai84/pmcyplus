package com.rcai.pm.audit;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserAccount actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "object_type", nullable = false, length = 40)
    private String objectType;

    @Column(name = "object_id")
    private Long objectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json")
    private String detailJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    AuditLog(UserAccount actor, String action, String objectType, Long objectId, String detailJson) {
        this.actor = actor;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.detailJson = detailJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getActor() { return actor; }
    public String getAction() { return action; }
    public String getObjectType() { return objectType; }
    public Long getObjectId() { return objectId; }
    public String getDetailJson() { return detailJson; }
    public Instant getCreatedAt() { return createdAt; }
}
