package com.openinsure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public final class CorrelationIdFilter extends OncePerRequestFilter {
    private final MeterRegistry meterRegistry;

    public CorrelationIdFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = validOrNew(request.getHeader("X-Correlation-Id"));
        String traceId = traceId(request.getHeader("traceparent"));
        String spanId = randomHex(16);
        long started = System.nanoTime();
        try (MDC.MDCCloseable ignored1 = MDC.putCloseable(CorrelationContext.CORRELATION_ID, correlationId);
                MDC.MDCCloseable ignored2 = MDC.putCloseable(CorrelationContext.TRACE_ID, traceId);
                MDC.MDCCloseable ignored3 = MDC.putCloseable(CorrelationContext.SPAN_ID, spanId)) {
            response.setHeader("X-Correlation-Id", correlationId);
            response.setHeader("traceparent", "00-" + traceId + "-" + spanId + "-01");
            try {
                chain.doFilter(request, response);
            } finally {
                Timer.builder("openinsure.http.server.duration")
                        .description("HTTP server request duration")
                        .tag("method", request.getMethod())
                        .tag("route", route(request))
                        .tag("status", Integer.toString(response.getStatus()))
                        .register(meterRegistry)
                        .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
                meterRegistry.counter("openinsure.http.server.requests", "method", request.getMethod(),
                        "route", route(request), "status", Integer.toString(response.getStatus())).increment();
            }
        }
    }

    private String validOrNew(String value) {
        return value != null && value.length() <= 100 && value.matches("[A-Za-z0-9._:-]+") ? value
                : CorrelationContext.newId();
    }

    private String traceId(String traceparent) {
        if (traceparent != null) {
            String[] parts = traceparent.split("-");
            if (parts.length == 4 && parts[1].matches("[0-9a-fA-F]{32}") && !parts[1].matches("0+"))
                return parts[1].toLowerCase();
        }
        return randomHex(32);
    }

    private String randomHex(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    private String route(HttpServletRequest request) {
        return request.getRequestURI().replaceAll("/[0-9a-fA-F-]{16,}", "/{id}");
    }
}
