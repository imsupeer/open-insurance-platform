package com.openinsure.policy.api;

import com.openinsure.policy.domain.PolicyEntity;
import java.util.UUID;

public final class PolicyDtos {
    private PolicyDtos() {
    }

    public record PolicyResponse(UUID id, String policyNumber, String ownerId, String product,
            PolicyEntity.Status status) {
        public static PolicyResponse from(PolicyEntity policy) {
            return new PolicyResponse(policy.getId(), policy.getPolicyNumber(), policy.getOwnerId(),
                    policy.getProduct(), policy.getStatus());
        }
    }
}
