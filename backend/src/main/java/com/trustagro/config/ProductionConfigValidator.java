package com.trustagro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Fail-fast production configuration checks. Messages never echo secret values.
 */
@Component
@Profile("prod")
public class ProductionConfigValidator {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${DB_URL:${spring.datasource.url:}}")
    private String dbUrl;

    @Value("${DB_USERNAME:${spring.datasource.username:}}")
    private String dbUsername;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${app.seed.demo-users:false}")
    private boolean seedDemoUsers;

    @Value("${app.seed.demo-master-data:false}")
    private boolean seedDemoMasterData;

    @Value("${app.seed.hr-reference-data:false}")
    private boolean seedHrReferenceData;

    @PostConstruct
    public void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "Production misconfiguration: APP_JWT_SECRET / app.jwt.secret is required and must be at least 32 characters");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "Production misconfiguration: APP_JWT_SECRET / app.jwt.secret must be at least 32 characters");
        }
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException(
                    "Production misconfiguration: DB_PASSWORD / spring.datasource.password is required");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException(
                    "Production misconfiguration: DB_URL / spring.datasource.url is required");
        }
        if (dbUrl.toLowerCase().contains("jdbc:h2")) {
            throw new IllegalStateException(
                    "Production misconfiguration: H2 database is not permitted in production profile");
        }
        if (dbUsername == null || dbUsername.isBlank()) {
            throw new IllegalStateException(
                    "Production misconfiguration: DB_USERNAME / spring.datasource.username is required");
        }
        if (seedDemoUsers) {
            throw new IllegalStateException(
                    "Production misconfiguration: app.seed.demo-users must be false in production");
        }
        if (seedDemoMasterData) {
            throw new IllegalStateException(
                    "Production misconfiguration: app.seed.demo-master-data must be false in production");
        }
        if (seedHrReferenceData) {
            throw new IllegalStateException(
                    "Production misconfiguration: app.seed.hr-reference-data must be false in production");
        }
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "Production misconfiguration: APP_CORS_ALLOWED_ORIGINS must list at least one explicit origin");
        }
        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            throw new IllegalStateException(
                    "Production misconfiguration: CORS must not use sole wildcard origin '*'");
        }
    }
}
