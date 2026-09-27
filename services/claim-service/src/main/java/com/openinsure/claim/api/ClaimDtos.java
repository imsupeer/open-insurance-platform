package com.openinsure.claim.api;

import com.openinsure.claim.domain.ClaimEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ClaimDtos {
    private ClaimDtos() {
    }

    public record CreateClaimRequest(@NotNull UUID policyId, @NotBlank @Size(max = 500) String description) {
    }

    public record ClaimResponse(UUID id, String principalId, UUID policyId, String description,
            ClaimEntity.Status status, ClaimEntity.ProcessingStatus processingStatus, Instant createdAt) {
        public static ClaimResponse from(ClaimEntity claim) {
            return new ClaimResponse(claim.getId(), claim.getPrincipalId(), claim.getPolicyId(), claim.getDescription(),
                    claim.getStatus(), claim.getProcessingStatus(), claim.getCreatedAt());
        }
    }
}
