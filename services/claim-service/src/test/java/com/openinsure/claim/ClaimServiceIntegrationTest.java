package com.openinsure.claim;

import com.openinsure.claim.api.ApiException;
import com.openinsure.claim.api.ClaimDtos;
import com.openinsure.claim.api.ClaimService;
import com.openinsure.claim.domain.OutboxEntity;
import com.openinsure.claim.domain.OutboxRepository;
import com.openinsure.claim.domain.OutboxStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = { "consent-service.url=http://127.0.0.1:1", "app.security.enabled=false",
        "app.kafka.enabled=false" })
@AutoConfigureMockMvc
class ClaimServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
            .withDatabaseName("openinsure").withUsername("openinsure").withPassword("openinsure-local-only")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ClaimService claimService;
    @Autowired
    OutboxRepository outboxRepository;

    @Test
    void claimRequiresConsentDecision() throws Exception {
        mvc.perform(post("/claims").header("X-Principal-Id", "customer-001")
                .header("X-Consent-Id", "22222222-2222-2222-2222-222222222222")
                .header("X-Scope", "claim:write").header("X-Purpose", "claim-assessment")
                .header("Idempotency-Key", "claim-key-1").contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"policyId\":\"11111111-1111-1111-1111-111111111111\",\"description\":\"synthetic incident\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void sameIdempotencyKeyReturnsSameClaimAndRejectsDifferentPayload() {
        String key = "claim-idempotency-" + UUID.randomUUID();
        ClaimDtos.CreateClaimRequest request = new ClaimDtos.CreateClaimRequest(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "synthetic incident");

        ClaimService.Creation first = claimService.create("customer-001", key, request);
        ClaimService.Creation repeated = claimService.create("customer-001", key, request);

        org.junit.jupiter.api.Assertions.assertTrue(first.created());
        org.junit.jupiter.api.Assertions.assertFalse(repeated.created());
        org.junit.jupiter.api.Assertions.assertEquals(first.claim().getId(), repeated.claim().getId());
        OutboxEntity outbox = outboxRepository.findAll().stream().findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(first.claim().getId(), outbox.getAggregateId());
        org.junit.jupiter.api.Assertions.assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        org.junit.jupiter.api.Assertions.assertThrows(ApiException.class, () -> claimService.create(
                "customer-001", key, new ClaimDtos.CreateClaimRequest(request.policyId(), "different payload")));
    }
}
