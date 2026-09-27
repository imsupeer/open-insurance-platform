package com.openinsure.consent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
@SpringBootTest(properties = "app.security.enabled=false")
@AutoConfigureMockMvc
class ConsentServiceIntegrationTest {
        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6")
                        .withDatabaseName("openinsure")
                        .withUsername("openinsure")
                        .withPassword("openinsure-local-only")
                        .withStartupTimeout(Duration.ofMinutes(2));

        @DynamicPropertySource
        static void database(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        MockMvc mvc;

        @Test
        void revokingConsentBlocksDecisionImmediately() throws Exception {
                String body = """
                                {"resourceOwnerId":"customer-001","purpose":"claim-assessment","scopes":["policy:read"],"expiresAt":"2099-01-01T00:00:00Z"}
                                """;
                String response = mvc.perform(post("/consents").header("X-Principal-Id", "customer-001")
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
                String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

                mvc.perform(get("/internal/consents/{id}/decision", id)
                                .param("principalId", "customer-001").param("resourceOwnerId", "customer-001")
                                .param("requiredScope", "policy:read").param("purpose", "claim-assessment"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true));

                mvc.perform(post("/consents/{id}/revoke", id).header("X-Principal-Id", "customer-001"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOKED"));

                mvc.perform(get("/internal/consents/{id}/decision", id)
                                .param("principalId", "customer-001").param("resourceOwnerId", "customer-001")
                                .param("requiredScope", "policy:read").param("purpose", "claim-assessment"))
                                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
}
