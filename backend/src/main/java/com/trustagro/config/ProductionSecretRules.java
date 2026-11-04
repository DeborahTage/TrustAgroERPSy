package com.trustagro.config;

import java.util.Locale;

final class ProductionSecretRules {

    private ProductionSecretRules() {
    }

    static boolean isPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("<")
                || normalized.contains("generate-")
                || normalized.contains("example.com")
                || normalized.contains("change-me")
                || normalized.contains("replace-me")
                || normalized.contains("your-secret")
                || normalized.contains("placeholder")
                || normalized.startsWith("testonly")
                || normalized.startsWith("localdev");
    }

    static boolean isExampleEmail(String value) {
        return value == null || value.isBlank()
                || value.trim().toLowerCase(Locale.ROOT).endsWith("@example.com");
    }
}
