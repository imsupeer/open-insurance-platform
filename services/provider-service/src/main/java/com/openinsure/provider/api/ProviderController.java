package com.openinsure.provider.api;

import com.openinsure.provider.domain.ProviderData;
import com.openinsure.provider.service.ProviderCatalog;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/providers")
public class ProviderController {
    private final ProviderCatalog catalog;

    public ProviderController(ProviderCatalog catalog) { this.catalog = catalog; }

    @GetMapping
    public List<ProviderData.Summary> list() {
        catalog.countRequest("list");
        return catalog.summaries();
    }

    @GetMapping("/{provider}")
    public ProviderData.Summary summary(@PathVariable String provider) {
        catalog.countRequest("summary");
        return catalog.summary(provider);
    }

    @GetMapping("/{provider}/policies")
    public List<ProviderData.Policy> policies(@PathVariable String provider) {
        catalog.countRequest("policies");
        return catalog.policies(provider);
    }

    @GetMapping("/{provider}/claims")
    public List<ProviderData.Claim> claims(@PathVariable String provider) {
        catalog.countRequest("claims");
        return catalog.claims(provider);
    }

    @ExceptionHandler(ProviderCatalog.ProviderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Problem problem(ProviderCatalog.ProviderNotFoundException exception) {
        return new Problem("https://openinsure.local/problems/provider-not-found",
                "Provider not found", 404, exception.getMessage());
    }

    public record Problem(String type, String title, int status, String detail) {}
}

