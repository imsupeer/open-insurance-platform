package com.openinsure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.openinsure.provider.service.ProviderCatalog;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ProviderCatalogTest {
    private final ProviderCatalog catalog = new ProviderCatalog(new SimpleMeterRegistry());

    @Test
    void exposesSyntheticProvidersWithProductionLikeFixtures() {
        assertThat(catalog.summaries()).hasSize(3)
                .extracting(summary -> summary.provider().slug())
                .containsExactly("itau", "bradesco-seguros", "banco-do-brasil");
        assertThat(catalog.summary("itau").dataClassification()).isEqualTo("fictitious");
        assertThat(catalog.summary("itau").environment()).isEqualTo("prod-like-local");
        assertThat(catalog.policies("bradesco-seguros")).hasSize(4);
        assertThat(catalog.claims("bradesco-seguros")).hasSize(3);
        assertThat(catalog.summary("banco-do-brasil").capabilities()).contains("premium-payments")
                .doesNotContain("claims");
    }

    @Test
    void exposesRequestMetric() {
        catalog.countRequest("claims");
        assertThat(catalog.summary("itau").policyCount()).isEqualTo(3);
    }
}

