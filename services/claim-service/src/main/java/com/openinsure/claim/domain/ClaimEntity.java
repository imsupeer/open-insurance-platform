package com.openinsure.claim.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class ClaimEntity {
    public enum Status {
        OPEN
    }

    @Id
    private UUID id;
    private String principalId;
    private UUID policyId;
    private String idempotencyKey;
    private String payloadHash;
    private String description;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
    private Instant createdAt;
    @Version
    private long version;

    protected ClaimEntity() {
    }

    public ClaimEntity(UUID id, String principalId, UUID policyId, String idempotencyKey,
            String payloadHash, String description, Instant createdAt) {
        this.id = id;
        this.principalId = principalId;
        this.policyId = policyId;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.description = description;
        this.status = Status.OPEN;
        this.processingStatus = ProcessingStatus.PENDING;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public enum ProcessingStatus {
        PENDING, PROCESSED, FAILED
    }
}
