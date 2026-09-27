package com.openinsure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    @Test
    void preservesCorrelationAndContinuesTraceContext() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter(new SimpleMeterRegistry());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/claims/123");
        request.addHeader("X-Correlation-Id", "demo-001");
        request.addHeader("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> called.set(true));

        assertTrue(called.get());
        assertEquals("demo-001", response.getHeader("X-Correlation-Id"));
        assertTrue(response.getHeader("traceparent").startsWith("00-0123456789abcdef0123456789abcdef-"));
    }
}
