package com.openinsure.consent.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consents")
public class ConsentEntity {
    @Id
    private UUID id;
    private String principalId;
    private String resourceOwnerId;
    private String purpose;
    private String scopes;
    private Instant expiresAt;
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;
    private Instant createdAt;
    private Instant revokedAt;

    protected ConsentEntity() {
    }

    public ConsentEntity(UUID id, String principalId, String resourceOwnerId, String purpose,
            List<String> scopes, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.principalId = principalId;
        this.resourceOwnerId = resourceOwnerId;
        this.purpose = purpose;
        this.scopes = String.join(",", scopes);
        this.expiresAt = expiresAt;
        this.status = ConsentStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public boolean isUsableAt(Instant now, String principal, String owner, String requiredScope,
            String requestedPurpose) {
        if (status == ConsentStatus.ACTIVE && !expiresAt.isAfter(now)) {
            status = ConsentStatus.EXPIRED;
        }
        return status == ConsentStatus.ACTIVE
                && principalId.equals(principal)
                && resourceOwnerId.equals(owner)
                && purpose.equals(requestedPurpose)
                && scopes().contains(requiredScope);
    }

    public void revoke(Instant now) {
        if (status == ConsentStatus.ACTIVE) {
            status = ConsentStatus.REVOKED;
            revokedAt = now;
        }
    }

    public List<String> scopes() {
        return Arrays.asList(scopes.split(","));
    }

    public UUID getId() {
        return id;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public String getPurpose() {
        return purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
