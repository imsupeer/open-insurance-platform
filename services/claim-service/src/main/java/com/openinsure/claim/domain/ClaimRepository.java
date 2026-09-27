package com.openinsure.claim.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {
    Optional<ClaimEntity> findByPrincipalIdAndIdempotencyKey(String principalId, String idempotencyKey);
}
