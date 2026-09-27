package com.openinsure.provider.service;

import com.openinsure.provider.adapter.SyntheticProvider;
import com.openinsure.provider.domain.ProviderData;
import com.openinsure.provider.domain.ProviderId;
import com.openinsure.provider.port.InsuranceProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProviderCatalog {
    private final Map<ProviderId, InsuranceProvider> providers = new EnumMap<>(ProviderId.class);
    private final MeterRegistry meterRegistry;

    public ProviderCatalog(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        SyntheticProvider.fixtures().forEach(provider -> {
            providers.put(provider.id(), provider);
            Tags tags = Tags.of("provider", provider.id().slug());
            meterRegistry.gauge("openinsure.provider.policies", tags, provider, value -> value.policies().size());
            meterRegistry.gauge("openinsure.provider.claims", tags, provider, value -> value.claims().size());
            meterRegistry.gauge("openinsure.provider.events", tags, provider, value -> value.summary().eventCount());
        });
    }

    public List<ProviderData.Summary> summaries() { return providers.values().stream().map(InsuranceProvider::summary).toList(); }
    public ProviderData.Summary summary(String slug) { return provider(slug).summary(); }
    public List<ProviderData.Policy> policies(String slug) { return provider(slug).policies(); }
    public List<ProviderData.Claim> claims(String slug) { return provider(slug).claims(); }
    public void countRequest(String operation) { meterRegistry.counter("openinsure.provider.requests", "operation", operation).increment(); }

    private InsuranceProvider provider(String slug) {
        try {
            return providers.get(ProviderId.fromSlug(slug));
        } catch (IllegalArgumentException exception) {
            throw new ProviderNotFoundException(slug);
        }
    }

    public static class ProviderNotFoundException extends RuntimeException {
        public ProviderNotFoundException(String slug) { super("Unknown provider: " + slug); }
    }
}

