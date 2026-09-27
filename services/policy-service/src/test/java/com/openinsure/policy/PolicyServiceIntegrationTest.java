package com.openinsure.policy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {"consent-service.url=http://127.0.0.1:1", "app.security.enabled=false"})
@AutoConfigureMockMvc
class PolicyServiceIntegrationTest {
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

    @Autowired MockMvc mvc;

    @Test
    void nonOwnerIsRejectedBeforeConsentLookup() throws Exception {
        mvc.perform(get("/policies/11111111-1111-1111-1111-111111111111")
                        .header("X-Principal-Id", "customer-999")
                        .header("X-Consent-Id", "22222222-2222-2222-2222-222222222222")
                        .header("X-Scope", "policy:read").header("X-Purpose", "claim-assessment"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }
}
