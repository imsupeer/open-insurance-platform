package com.openinsure.provider.adapter;

import com.openinsure.provider.domain.ProviderData;
import com.openinsure.provider.domain.ProviderId;
import com.openinsure.provider.port.InsuranceProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SyntheticProvider implements InsuranceProvider {
    private final ProviderId id;
    private final List<ProviderData.Policy> policies;
    private final List<ProviderData.Claim> claims;
    private final int eventCount;

    public SyntheticProvider(ProviderId id, List<ProviderData.Policy> policies,
            List<ProviderData.Claim> claims, int eventCount) {
        this.id = id;
        this.policies = List.copyOf(policies);
        this.claims = List.copyOf(claims);
        this.eventCount = eventCount;
    }

    @Override public ProviderId id() { return id; }
    @Override public List<ProviderData.Policy> policies() { return policies; }
    @Override public List<ProviderData.Claim> claims() { return claims; }

    @Override
    public ProviderData.Summary summary() {
        List<String> capabilities = new ArrayList<>();
        if (id.supportsInsurance()) capabilities.addAll(List.of("insurance-data", "claims"));
        if (id.supportsPayments()) capabilities.add("premium-payments");
        capabilities.add("consent-aware-sharing");
        return new ProviderData.Summary(id, id.displayName(), id.category(), "prod-like-local",
                "fictitious", List.copyOf(capabilities), policies.size(), claims.size(), eventCount, "UP");
    }

    public static List<InsuranceProvider> fixtures() {
        return List.of(
            new SyntheticProvider(ProviderId.ITAU, List.of(
                policy("itau-pol-1001", "ITAU-AUTO-2026-1001", "Auto", "ACTIVE", "289.90", "customer-demo-001"),
                policy("itau-pol-1002", "ITAU-RES-2026-1002", "Residencial", "ACTIVE", "119.90", "customer-demo-002"),
                policy("itau-pol-1003", "ITAU-VIDA-2026-1003", "Vida", "PENDING", "84.50", "customer-demo-003")),
                List.of(
                    claim("itau-clm-2001", "ITAU-CLAIM-2001", "Auto", "IN_REVIEW", "6800.00", "2026-09-27T08:10:00Z"),
                    claim("itau-clm-2002", "ITAU-CLAIM-2002", "Residencial", "APPROVED", "2100.00", "2026-09-26T15:20:00Z")), 41),
            new SyntheticProvider(ProviderId.BRADESCO_SEGUROS, List.of(
                policy("brad-pol-3001", "BRAD-AUTO-2026-3001", "Auto", "ACTIVE", "319.90", "customer-demo-001"),
                policy("brad-pol-3002", "BRAD-RES-2026-3002", "Residencial", "ACTIVE", "149.90", "customer-demo-004"),
                policy("brad-pol-3003", "BRAD-VIAGEM-2026-3003", "Viagem", "ACTIVE", "59.90", "customer-demo-005"),
                policy("brad-pol-3004", "BRAD-VIDA-2026-3004", "Vida", "CANCELLED", "99.90", "customer-demo-006")),
                List.of(
                    claim("brad-clm-4001", "BRAD-CLAIM-4001", "Auto", "IN_REVIEW", "4200.00", "2026-09-27T07:45:00Z"),
                    claim("brad-clm-4002", "BRAD-CLAIM-4002", "Viagem", "PAID", "980.00", "2026-09-25T10:30:00Z"),
                    claim("brad-clm-4003", "BRAD-CLAIM-4003", "Residencial", "DOCUMENTS_REQUIRED", "3600.00", "2026-09-24T12:00:00Z")), 34),
            new SyntheticProvider(ProviderId.BANCO_DO_BRASIL, List.of(), List.of(), 28));
    }

    private static ProviderData.Policy policy(String id, String number, String product, String status,
            String premium, String owner) {
        return new ProviderData.Policy(id, number, product, status, new BigDecimal(premium), owner);
    }

    private static ProviderData.Claim claim(String id, String number, String product, String status,
            String amount, String reportedAt) {
        return new ProviderData.Claim(id, number, product, status, new BigDecimal(amount),
                Instant.parse(reportedAt));
    }
}

