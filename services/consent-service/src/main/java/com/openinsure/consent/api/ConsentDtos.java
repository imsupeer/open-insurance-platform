package com.openinsure.consent.api;

import com.openinsure.consent.domain.ConsentEntity;
import com.openinsure.consent.domain.ConsentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ConsentDtos {
    private ConsentDtos() {
    }

    public record CreateConsentRequest(
            @NotBlank String resourceOwnerId,
            @NotBlank String purpose,
            @NotEmpty @Size(max = 20) List<@NotBlank String> scopes,
            @Future Instant expiresAt) {
    }

    public record ConsentResponse(UUID id, String principalId, String resourceOwnerId, String purpose,
            List<String> scopes, Instant expiresAt, ConsentStatus status,
            Instant createdAt, Instant revokedAt) {
        public static ConsentResponse from(ConsentEntity consent) {
            return new ConsentResponse(consent.getId(), consent.getPrincipalId(), consent.getResourceOwnerId(),
                    consent.getPurpose(), consent.scopes(), consent.getExpiresAt(), consent.getStatus(),
                    consent.getCreatedAt(), consent.getRevokedAt());
        }
    }

    public record DecisionResponse(boolean allowed, UUID consentId) {
    }
}
