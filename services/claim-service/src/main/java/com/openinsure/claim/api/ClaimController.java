package com.openinsure.claim.api;

import com.openinsure.claim.client.ConsentDecisionClient;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

@RestController
public class ClaimController {
    private final ClaimService service;
    private final ConsentDecisionClient consentClient;

    public ClaimController(ClaimService service, ConsentDecisionClient consentClient) {
        this.service = service;
        this.consentClient = consentClient;
    }

    @PostMapping("/claims")
    ResponseEntity<ClaimDtos.ClaimResponse> create(
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Consent-Id") UUID consentId, @RequestHeader("X-Scope") String scope,
            @RequestHeader("X-Purpose") String purpose, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody ClaimDtos.CreateClaimRequest request) {
        String principalId = principal(jwt, principalHeader);
        consentClient.require(consentId, principalId, scope, purpose);
        ClaimService.Creation creation = service.create(principalId, key, request);
        return ResponseEntity.status(creation.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ClaimDtos.ClaimResponse.from(creation.claim()));
    }

    @GetMapping("/claims/{claimId}")
    ClaimDtos.ClaimResponse get(@PathVariable UUID claimId,
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt) {
        String principalId = principal(jwt, principalHeader);
        return ClaimDtos.ClaimResponse.from(service.get(claimId, principalId));
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
