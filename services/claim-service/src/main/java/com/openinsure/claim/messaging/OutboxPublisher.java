package com.openinsure.claim.messaging;

import com.openinsure.claim.domain.OutboxEntity;
import com.openinsure.claim.domain.OutboxRepository;
import com.openinsure.claim.domain.OutboxStatus;
import java.time.Duration;
import java.time.Instant;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    public OutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEntity event : repository.findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                OutboxStatus.PENDING, Instant.now())) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload())
                        .get(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                event.markPublished(Instant.now());
                meterRegistry.counter("openinsure.outbox.published", "topic", event.getTopic()).increment();
                log.info("outbox event published topic={} aggregateId={}", event.getTopic(), event.getAggregateId());
            } catch (Exception exception) {
                event.markRetry(Instant.now().plusSeconds(Math.min(30, 1L << Math.min(event.getAttempts(), 5))),
                        exception.getClass().getSimpleName());
                meterRegistry.counter("openinsure.outbox.retry", "topic", event.getTopic()).increment();
                log.warn("outbox publish retry topic={} aggregateId={} reason={}", event.getTopic(),
                        event.getAggregateId(), exception.getClass().getSimpleName());
            }
        }
    }
}
