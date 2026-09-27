package com.openinsure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication
public class ObservabilityConfiguration {
    @Bean
    Filter correlationIdFilter(MeterRegistry meterRegistry) {
        return new CorrelationIdFilter(meterRegistry);
    }
}
