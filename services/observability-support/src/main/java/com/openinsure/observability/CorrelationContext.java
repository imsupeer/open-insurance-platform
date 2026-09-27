package com.openinsure.observability;

import java.util.UUID;
import org.slf4j.MDC;

public final class CorrelationContext {
    public static final String CORRELATION_ID = "correlationId";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    private CorrelationContext() {
    }

    public static String correlationId() {
        return MDC.get(CORRELATION_ID);
    }

    public static String traceId() {
        return MDC.get(TRACE_ID);
    }

    public static String spanId() {
        return MDC.get(SPAN_ID);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
