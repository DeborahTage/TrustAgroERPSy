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
        setValidDatabaseConfig(validator);
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
        setValidDatabaseConfig(validator);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("32 characters"));
    }

    @Test
    void blankDbPasswordFailsFast() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "  ");
        setValidDatabaseConfig(validator);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    void blankDbUrlFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", " ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("DB_URL"));
    }

    @Test
    void h2DbUrlInProdFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:h2:mem:test");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("H2 database is not permitted in production profile"));
    }

    @Test
    void blankDbUsernameFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "dbUsername", " ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("DB_USERNAME"));
    }

    @Test
    void soleWildcardCorsFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "*");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().toLowerCase().contains("wildcard"));
    }

    @Test
    void emptyCorsFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "  ,  ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS"));
    }

    @Test
    void validConfigPasses() {
        ProductionConfigValidator validator = validValidator();
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void demoUserSeedInProdFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "seedDemoUsers", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("demo-users"));
    }

    @Test
    void demoMasterDataSeedInProdFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "seedDemoMasterData", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("demo-master-data"));
    }

    @Test
    void hrReferenceDataSeedInProdFailsFast() {
        ProductionConfigValidator validator = validValidator();
        ReflectionTestUtils.setField(validator, "seedHrReferenceData", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("hr-reference-data"));
    }

    private ProductionConfigValidator validValidator() {
        ProductionConfigValidator validator = new ProductionConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret", "LocalDevOnlyTrustAgroJwtSecretKeyMustBeLongEnough123");
        ReflectionTestUtils.setField(validator, "dbPassword", "secure-db-password");
        setValidDatabaseConfig(validator);
        ReflectionTestUtils.setField(validator, "corsAllowedOrigins", "https://app.example.com");
        ReflectionTestUtils.setField(validator, "seedDemoUsers", false);
        ReflectionTestUtils.setField(validator, "seedDemoMasterData", false);
        ReflectionTestUtils.setField(validator, "seedHrReferenceData", false);
        return validator;
    }

    private void setValidDatabaseConfig(ProductionConfigValidator validator) {
        ReflectionTestUtils.setField(validator, "dbUrl", "jdbc:postgresql://localhost:5432/trust_agro_test");
        ReflectionTestUtils.setField(validator, "dbUsername", "trustagro_test");
    }
}
