package com.openinsure.policy.api;

import com.openinsure.policy.client.ConsentDecisionClient;
import com.openinsure.policy.domain.PolicyEntity;
import com.openinsure.policy.domain.PolicyRepository;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
public class PolicyController {
    private final PolicyRepository repository;
    private final ConsentDecisionClient consentClient;
    private final MeterRegistry meterRegistry;

    public PolicyController(PolicyRepository repository, ConsentDecisionClient consentClient,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.consentClient = consentClient;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/policies/{policyId}")
    PolicyDtos.PolicyResponse get(@PathVariable UUID policyId,
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Consent-Id") UUID consentId,
            @RequestHeader("X-Scope") String scope,
            @RequestHeader("X-Purpose") String purpose) {
        String principalId = principal(jwt, principalHeader);
        PolicyEntity policy = repository.findById(policyId)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Policy not found"));
        if (!policy.getOwnerId().equals(principalId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Principal is not the policy owner");
        }
        consentClient.require(consentId, principalId, policy.getOwnerId(), scope, purpose);
        meterRegistry.counter("openinsure.policies.accessed", "product", policy.getProduct()).increment();
        return PolicyDtos.PolicyResponse.from(policy);
    }

    private String principal(Jwt jwt, String principalHeader) {
        if (jwt == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
                String username = principal.getAttribute("preferred_username");
                return username != null ? username : principal.getName();
            }
            if (authentication instanceof JwtAuthenticationToken token) {
                jwt = token.getToken();
            }
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                if (jwt == null)
                    return authentication.getName();
            }
        }
        if (jwt != null) {
            String username = jwt.getClaimAsString("preferred_username");
            return username != null ? username : jwt.getSubject();
        }
        return principalHeader;
    }
}
