package com.openinsure.provider.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProviderData {
    private ProviderData() {}

    public record Policy(String id, String policyNumber, String product, String status,
                         BigDecimal premium, String ownerReference) {}
    public record Claim(String id, String claimNumber, String product, String status,
                        BigDecimal amount, Instant reportedAt) {}
    public record Summary(ProviderId provider, String displayName, String category,
                          String environment, String dataClassification, List<String> capabilities,
                          int policyCount, int claimCount, int eventCount, String health) {}
}

