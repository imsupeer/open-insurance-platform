package com.openinsure.provider.domain;

import java.util.Locale;

public enum ProviderId {
    ITAU("itau", "Itaú Unibanco", "insurance-and-banking", true, true),
    BRADESCO_SEGUROS("bradesco-seguros", "Bradesco Seguros", "insurance", true, false),
    BANCO_DO_BRASIL("banco-do-brasil", "Banco do Brasil", "banking-and-payments", false, true);

    private final String slug;
    private final String displayName;
    private final String category;
    private final boolean insurance;
    private final boolean payments;

    ProviderId(String slug, String displayName, String category, boolean insurance, boolean payments) {
        this.slug = slug;
        this.displayName = displayName;
        this.category = category;
        this.insurance = insurance;
        this.payments = payments;
    }
    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String category() { return category; }
    public boolean supportsInsurance() { return insurance; }
    public boolean supportsPayments() { return payments; }

    public static ProviderId fromSlug(String slug) {
        String normalized = slug.toLowerCase(Locale.ROOT);
        for (ProviderId provider : values()) {
            if (provider.slug.equals(normalized)) return provider;
        }
        throw new IllegalArgumentException("Unknown provider: " + slug);
    }
}

