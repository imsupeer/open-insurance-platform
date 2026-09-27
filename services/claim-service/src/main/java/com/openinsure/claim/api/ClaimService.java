package com.openinsure.claim.api;

import com.openinsure.claim.domain.ClaimEntity;
import com.openinsure.claim.domain.ClaimRepository;
import com.openinsure.claim.domain.OutboxEntity;
import com.openinsure.claim.domain.OutboxRepository;
import com.openinsure.observability.CorrelationContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class ClaimService {
    private final ClaimRepository repository;
    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    public ClaimService(ClaimRepository repository, OutboxRepository outboxRepository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Creation create(String principalId, String idempotencyKey, ClaimDtos.CreateClaimRequest request) {
        String hash = hash(request);
        var existing = repository.findByPrincipalIdAndIdempotencyKey(principalId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getPayloadHash().equals(hash)) {
                throw new ApiException(HttpStatus.CONFLICT, "Idempotency-Key was reused with a different payload");
            }
            meterRegistry.counter("openinsure.claims.idempotent_replays").increment();
            return new Creation(existing.get(), false);
        }
        ClaimEntity claim = new ClaimEntity(UUID.randomUUID(), principalId, request.policyId(), idempotencyKey,
                hash, request.description(), Instant.now());
        ClaimEntity saved = repository.save(claim);
        Instant now = Instant.now();
        UUID eventId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\",\"aggregateId\":\"" + saved.getId()
                + "\",\"occurredAt\":\"" + now + "\",\"schemaVersion\":1,\"claimId\":\""
                + saved.getId() + "\",\"policyId\":\"" + saved.getPolicyId() + "\",\"correlationId\":\""
                + safeCorrelationId() + "\"}";
        outboxRepository.save(new OutboxEntity(UUID.randomUUID(), eventId, saved.getId(), "claim-events",
                saved.getId().toString(), "ClaimCreated", payload, now));
        meterRegistry.counter("openinsure.claims.created").increment();
        return new Creation(saved, true);
    }

    private String safeCorrelationId() {
        return CorrelationContext.correlationId() == null ? UUID.randomUUID().toString()
                : CorrelationContext.correlationId();
    }

    public ClaimEntity get(UUID id, String principalId) {
        ClaimEntity claim = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        if (!claim.getPrincipalId().equals(principalId))
            throw new ApiException(HttpStatus.FORBIDDEN, "Principal cannot access this claim");
        return claim;
    }

    private String hash(ClaimDtos.CreateClaimRequest request) {
        try {
            byte[] canonicalPayload = (request.policyId() + "\u0000" + request.description())
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalPayload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not hash claim payload", exception);
        }
    }

    public record Creation(ClaimEntity claim, boolean created) {
    }
}
