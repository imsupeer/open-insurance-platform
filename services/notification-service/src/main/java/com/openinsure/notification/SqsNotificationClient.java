package com.openinsure.notification;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class SqsNotificationClient implements AutoCloseable {
    private final SqsClient client;
    private final String queueName;
    private final MeterRegistry meterRegistry;
    private final AtomicReference<String> queueUrl = new AtomicReference<>();

    public SqsNotificationClient(@Value("${aws.endpoint:http://localhost:4566}") String endpoint,
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.access-key-id:local}") String accessKey,
            @Value("${aws.secret-access-key:local-only}") String secretKey,
            @Value("${notification.queue-name:openinsure-claim-notifications.fifo}") String queueName,
            MeterRegistry meterRegistry) {
        this.client = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        this.queueName = queueName;
        this.meterRegistry = meterRegistry;
    }

    public void ensureQueue() {
        queueUrl.updateAndGet(current -> current == null ? client.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .attributes(Map.of(
                        QueueAttributeName.FIFO_QUEUE, "true",
                        QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false"))
                .build()).queueUrl() : current);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeQueue() {
        ensureQueue();
    }

    public void send(String eventId, String claimId, String body) {
        ensureQueue();
        client.sendMessage(SendMessageRequest.builder()
                .queueUrl(Objects.requireNonNull(queueUrl.get()))
                .messageBody(body)
                .messageGroupId(claimId)
                .messageDeduplicationId(eventId)
                .build());
        meterRegistry.counter("openinsure.notifications.sent").increment();
    }

    @Override
    public void close() {
        client.close();
    }
}
