package com.openinsure.policy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class PolicyEntity {
    public enum Status {
        ACTIVE, CANCELLED
    }

    @Id
    private UUID id;
    private String policyNumber;
    private String ownerId;
    private String product;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected PolicyEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getProduct() {
        return product;
    }

    public Status getStatus() {
        return status;
    }
}
