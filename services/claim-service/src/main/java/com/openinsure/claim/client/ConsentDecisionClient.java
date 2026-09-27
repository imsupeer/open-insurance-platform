package com.openinsure.claim.client;

import com.openinsure.claim.api.ApiException;
import com.openinsure.observability.CorrelationContext;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ConsentDecisionClient {
    private final RestClient client;

    public ConsentDecisionClient(@Value("${consent-service.url:http://localhost:8081}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void require(UUID consentId, String principalId, String scope, String purpose) {
        try {
            var request = client.get().uri(uriBuilder -> uriBuilder.path("/internal/consents/{id}/decision")
                    .queryParam("principalId", principalId).queryParam("resourceOwnerId", principalId)
                    .queryParam("requiredScope", scope).queryParam("purpose", purpose)
                    .build(consentId));
            String token = bearerToken();
            if (token != null) {
                request.headers(headers -> headers.setBearerAuth(token));
            }
            request.headers(headers -> {
                if (CorrelationContext.correlationId() != null)
                    headers.set("X-Correlation-Id", CorrelationContext.correlationId());
                if (CorrelationContext.traceId() != null && CorrelationContext.spanId() != null)
                    headers.set("traceparent",
                            "00-" + CorrelationContext.traceId() + "-" + CorrelationContext.spanId() + "-01");
            });
            request.retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError())
                throw new ApiException(HttpStatus.FORBIDDEN, "Consent does not authorize this access");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Consent service unavailable");
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Consent service unavailable");
        }
    }

    private String bearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        return null;
    }
}
