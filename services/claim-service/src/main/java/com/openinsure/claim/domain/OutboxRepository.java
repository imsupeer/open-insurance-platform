package com.openinsure.claim.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEntity, java.util.UUID> {
    List<OutboxEntity> findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status,
            Instant availableAt);
}
