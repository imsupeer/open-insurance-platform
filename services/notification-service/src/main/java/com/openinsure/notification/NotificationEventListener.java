package com.openinsure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openinsure.observability.CorrelationContext;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private final SqsNotificationClient sqs;
    private final ObjectMapper mapper;

    public NotificationEventListener(SqsNotificationClient sqs, ObjectMapper mapper) {
        this.sqs = sqs;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${app.kafka.notification-topic:claim-notifications}", groupId = "${spring.kafka.consumer.group-id:notification-service-v1}")
    public void consume(String payload) throws Exception {
        JsonNode event = mapper.readTree(payload);
        String eventId = required(event, "eventId");
        String claimId = required(event, "claimId");
        String correlationId = event.path("correlationId").asText(eventId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(CorrelationContext.CORRELATION_ID, correlationId)) {
            sqs.send(eventId, claimId, payload);
        }
    }

    private String required(JsonNode event, String field) {
        String value = event.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing notification field: " + field);
        }
        if ("eventId".equals(field)) {
            UUID.fromString(value);
        }
        return value;
    }
}
