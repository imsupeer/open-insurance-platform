package com.openinsure.consent.api;

import com.openinsure.consent.domain.ConsentEntity;
import com.openinsure.consent.domain.ConsentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import jakarta.validation.Valid;
import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping
@Validated
public class ConsentController {
    private final ConsentRepository repository;
    private final MeterRegistry meterRegistry;

    public ConsentController(ConsentRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/consents")
    ResponseEntity<ConsentDtos.ConsentResponse> create(
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConsentDtos.CreateConsentRequest request) {
        String principalId = principal(jwt, principalHeader);
        ConsentEntity consent = new ConsentEntity(UUID.randomUUID(), principalId, request.resourceOwnerId(),
                request.purpose(), request.scopes(), request.expiresAt(), Instant.now());
        meterRegistry.counter("openinsure.consents.created").increment();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConsentDtos.ConsentResponse.from(repository.save(consent)));
    }

    @GetMapping("/consents/{consentId}")
    ConsentDtos.ConsentResponse get(@PathVariable UUID consentId,
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt) {
        String principalId = principal(jwt, principalHeader);
        ConsentEntity consent = find(consentId);
        ensurePrincipal(consent, principalId);
        return ConsentDtos.ConsentResponse.from(consent);
    }

    @PostMapping("/consents/{consentId}/revoke")
    ConsentDtos.ConsentResponse revoke(@PathVariable UUID consentId,
            @RequestHeader(value = "X-Principal-Id", required = false) String principalHeader,
            @AuthenticationPrincipal Jwt jwt) {
        String principalId = principal(jwt, principalHeader);
        ConsentEntity consent = find(consentId);
        ensurePrincipal(consent, principalId);
        consent.revoke(Instant.now());
        meterRegistry.counter("openinsure.consents.revoked").increment();
        return ConsentDtos.ConsentResponse.from(repository.save(consent));
    }

    @GetMapping("/internal/consents/{consentId}/decision")
    ConsentDtos.DecisionResponse decision(@PathVariable UUID consentId,
            @RequestParam String principalId, @RequestParam String resourceOwnerId,
            @RequestParam String requiredScope, @RequestParam String purpose) {
        ConsentEntity consent = find(consentId);
        boolean allowed = consent.isUsableAt(Instant.now(), principalId, resourceOwnerId, requiredScope, purpose);
        repository.save(consent);
        meterRegistry.counter("openinsure.consents.decisions", "result", allowed ? "allowed" : "denied").increment();
        if (!allowed)
            throw new ApiException(HttpStatus.FORBIDDEN, "Consent does not authorize this access");
        return new ConsentDtos.DecisionResponse(true, consent.getId());
    }

    private ConsentEntity find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consent not found"));
    }

    private void ensurePrincipal(ConsentEntity consent, String principalId) {
        if (!consent.getPrincipalId().equals(principalId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Principal cannot access this consent");
        }
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
