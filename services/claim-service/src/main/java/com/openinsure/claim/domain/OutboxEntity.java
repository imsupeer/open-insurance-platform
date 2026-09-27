package com.openinsure.claim.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_outbox")
public class OutboxEntity {
    @Id
    private UUID id;
    private UUID eventId;
    private UUID aggregateId;
    private String topic;
    private String eventKey;
    private String eventType;
    @Column(columnDefinition = "TEXT")
    private String payload;
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private int attempts;
    private Instant availableAt;
    private Instant createdAt;
    private Instant publishedAt;
    private String lastError;

    protected OutboxEntity() {
    }

    public OutboxEntity(UUID id, UUID eventId, UUID aggregateId, String topic, String eventKey,
            String eventType, String payload, Instant now) {
        this.id = id;
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.availableAt = now;
        this.createdAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void markPublished(Instant when) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = when;
    }

    public void markRetry(Instant when, String error) {
        attempts++;
        availableAt = when;
        lastError = error;
    }
}
