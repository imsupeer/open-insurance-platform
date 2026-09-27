package com.openinsure.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@ExtendWith(MockitoExtension.class)
class ClaimEventListenerTest {
    @Mock
    JdbcTemplate jdbc;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void duplicateEventDoesNotApplyEffectTwice() {
        ClaimEventListener listener = new ClaimEventListener(jdbc, "");
        String event = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"claimId\":\""
                + UUID.randomUUID() + "\"}";
        when(jdbc.update(startsWith("INSERT"), any(Object[].class)))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("processed event already exists"));
        when(jdbc.update(startsWith("UPDATE"), any(Object[].class))).thenReturn(1);

        listener.consume(event);
        listener.consume(event);

        verify(jdbc, times(1)).update(startsWith("UPDATE claims"), any(Object[].class));
    }

    @Test
    void dltPublishesSanitizedNotification() {
        ClaimEventListener listener = new ClaimEventListener(jdbc, "",
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), kafkaTemplate, "claim-notifications");
        UUID eventId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        String event = "{\"eventId\":\"" + eventId + "\",\"claimId\":\"" + claimId + "\"}";

        listener.deadLetter(new ConsumerRecord<>("claim-events", 0, 0L, claimId.toString(), event),
                new IllegalStateException("transient"));

        verify(kafkaTemplate).send("claim-notifications", claimId.toString(),
                "{\"schemaVersion\":1,\"eventId\":\"" + eventId + "\",\"claimId\":\"" + claimId
                        + "\",\"failureReason\":\"IllegalStateException\"}");
    }
}
