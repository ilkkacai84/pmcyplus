package com.rcai.pm.merge;

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

@Entity
@Table(name = "merge_records")
public class MergeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MergeObjectType objectType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceIdsJson;
    @Column(nullable = false)
    private Long targetId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private UserAccount operator;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mappingData;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MergeRecord() {}

    public MergeRecord(MergeObjectType objectType, String sourceIdsJson, Long targetId,
                       UserAccount operator, String mappingData) {
        this.objectType = objectType;
        this.sourceIdsJson = sourceIdsJson;
        this.targetId = targetId;
        this.operator = operator;
        this.mappingData = mappingData;
        this.status = "COMPLETED";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public MergeObjectType getObjectType() { return objectType; }
    public String getSourceIdsJson() { return sourceIdsJson; }
    public Long getTargetId() { return targetId; }
    public UserAccount getOperator() { return operator; }
    public String getMappingData() { return mappingData; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
