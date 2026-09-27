package com.openinsure.notification;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationEventListenerTest {
    @Test
    void forwardsOnlyContractPayloadToSqs() throws Exception {
        SqsNotificationClient sqs = Mockito.mock(SqsNotificationClient.class);
        NotificationEventListener listener = new NotificationEventListener(sqs, new ObjectMapper());
        String payload = "{\"schemaVersion\":1,\"eventId\":\"11111111-1111-1111-1111-111111111111\",\"claimId\":\"22222222-2222-2222-2222-222222222222\",\"failureReason\":\"IllegalStateException\"}";

        listener.consume(payload);

        verify(sqs).send("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222", payload);
    }
}
