package com.trustagro.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigValidatorTest {

    @Test
    void missingJwtSecretFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("APP_JWT_SECRET"));
        assertFalse(ex.getMessage().toLowerCase().contains("secure-db-password"));
    }

    @Test
    void shortJwtSecretFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "too-short");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("32 characters"));
    }

    @Test
    void blankDbPasswordFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "  ");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void soleWildcardCorsFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "*");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().toLowerCase().contains("wildcard"));
    }

    @Test
    void emptyCorsFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "  ,  ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS"));
    }

    @Test
    void validConfigPasses() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");
        ReflectionTestUtils.setField(validator, "seedDemoUsers", false);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void demoUserSeedInProdFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");
        ReflectionTestUtils.setField(validator, "seedDemoUsers", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("demo-users"));
    }
}
