package com.openinsure.worker;

import com.openinsure.observability.CorrelationContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ClaimEventListener {
    private static final String CONSUMER = "claim-worker-v1";
    private static final Pattern EVENT_ID = Pattern.compile("\\\"eventId\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern CLAIM_ID = Pattern.compile("\\\"claimId\\\":\\\"([^\\\"]+)\\\"");

    private final JdbcTemplate jdbc;
    private final String failOnClaimId;
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, String> notificationTemplate;
    private final String notificationTopic;
    private final AtomicInteger syntheticConsumerLag = new AtomicInteger();
    private static final Logger log = LoggerFactory.getLogger(ClaimEventListener.class);

    public ClaimEventListener(JdbcTemplate jdbc,
            @Value("${worker.fail-on-claim-id:}") String failOnClaimId) {
        this(jdbc, failOnClaimId, new SimpleMeterRegistry(), null, "claim-notifications");
    }

    @Autowired
    public ClaimEventListener(JdbcTemplate jdbc, @Value("${worker.fail-on-claim-id:}") String failOnClaimId,
            MeterRegistry meterRegistry, KafkaTemplate<String, String> notificationTemplate,
            @Value("${app.kafka.notification-topic:claim-notifications}") String notificationTopic) {
        this.jdbc = jdbc;
        this.failOnClaimId = failOnClaimId;
        this.meterRegistry = meterRegistry;
        this.notificationTemplate = notificationTemplate;
        this.notificationTopic = notificationTopic;
        Gauge.builder("openinsure.worker.consumer.lag", syntheticConsumerLag, AtomicInteger::get)
                .description("Synthetic local indicator: zero means the demo consumer is caught up")
                .register(this.meterRegistry);
    }

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 500, multiplier = 2, maxDelay = 2000), autoCreateTopics = "true")
    @KafkaListener(topics = "${app.kafka.claim-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(String payload) {
        syntheticConsumerLag.set(1);
        try {
            UUID eventId = UUID.fromString(required(payload, EVENT_ID, "eventId"));
            UUID claimId = UUID.fromString(required(payload, CLAIM_ID, "claimId"));
            String correlationId = optional(payload, "correlationId");
            try (MDC.MDCCloseable ignored = MDC.putCloseable(CorrelationContext.CORRELATION_ID,
                    correlationId == null ? eventId.toString() : correlationId)) {
                consumeInContext(eventId, claimId);
            }
        } finally {
            syntheticConsumerLag.set(0);
        }
    }

    private void consumeInContext(UUID eventId, UUID claimId) {
        if (claimId.toString().equals(failOnClaimId)) {
            throw new IllegalStateException("configured transient worker failure");
        }
        try {
            jdbc.update(
                    "INSERT INTO processed_claim_events (consumer_name, event_id, claim_id, processed_at) VALUES (?, ?, ?, ?)",
                    CONSUMER, eventId, claimId, Timestamp.from(Instant.now()));
        } catch (DuplicateKeyException duplicate) {
            return;
        }
        jdbc.update("UPDATE claims SET processing_status = 'PROCESSED' WHERE id = ?", claimId);
        meterRegistry.counter("openinsure.worker.events.processed").increment();
        log.info("claim event processed claimId={} eventId={}", claimId, eventId);
    }

    @org.springframework.kafka.annotation.DltHandler
    @Transactional
    public void deadLetter(ConsumerRecord<String, String> record, Exception exception) {
        UUID eventId = tryUuid(record.value(), EVENT_ID);
        jdbc.update(
                "INSERT INTO claim_event_dlt (id, event_id, payload, failure_reason, failed_at) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), eventId, record.value(), safeReason(exception), Timestamp.from(Instant.now()));
        meterRegistry.counter("openinsure.worker.events.dlt").increment();
        publishFailureNotification(eventId, tryUuid(record.value(), CLAIM_ID), safeReason(exception),
                optional(record.value(), "correlationId"));
        log.error("claim event moved to dlt eventId={} reason={}", eventId, safeReason(exception));
    }

    private void publishFailureNotification(UUID eventId, UUID claimId, String reason, String correlationId) {
        if (notificationTemplate == null) {
            return;
        }
        String effectiveClaimId = claimId == null ? (eventId == null ? "unknown" : eventId.toString())
                : claimId.toString();
        String effectiveEventId = eventId == null ? UUID.randomUUID().toString() : eventId.toString();
        String payload = "{\"schemaVersion\":1,\"eventId\":\"" + effectiveEventId
                + "\",\"claimId\":\"" + effectiveClaimId + "\",\"failureReason\":\""
                + reason + "\"" + (correlationId == null ? "" : ",\"correlationId\":\"" + correlationId + "\"") + "}";
        notificationTemplate.send(notificationTopic, effectiveClaimId, payload);
        meterRegistry.counter("openinsure.worker.notifications.published").increment();
    }

    private String required(String payload, Pattern pattern, String field) {
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find())
            throw new IllegalArgumentException("missing event field: " + field);
        return matcher.group(1);
    }

    private UUID tryUuid(String payload, Pattern pattern) {
        try {
            return UUID.fromString(required(payload, pattern, "eventId"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String optional(String payload, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\":\\\"([^\\\"]+)\\\"").matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String safeReason(Exception exception) {
        String reason = exception.getClass().getSimpleName();
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
