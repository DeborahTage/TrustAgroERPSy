package com.trustagro.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealPostgresConnectionGuardTest {

    @Test
    void acceptsGitHubActionsPostgresServiceUrl() {
        assertDoesNotThrow(() ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://localhost:5432/trust_agro_real_pg_test"));
    }

    @Test
    void acceptsLoopbackAddressForLocalPostgres() {
        assertDoesNotThrow(() ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://127.0.0.1:5432/trust_agro_real_pg_test"));
    }

    @Test
    void rejectsUnexpectedExternalHostEvenForDedicatedDatabaseName() {
        assertThrows(IllegalArgumentException.class, () ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://database.example.com:5432/trust_agro_real_pg_test"));
    }

    @Test
    void rejectsJdbcHostOverrideEvenWhenAuthorityIsLocalhost() {
        assertThrows(IllegalArgumentException.class, () ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://localhost:5432/trust_agro_real_pg_test?host=database.example.com"));
    }

    @Test
    void rejectsJdbcHostAddressOverrideEvenWhenAuthorityIsLocalhost() {
        assertThrows(IllegalArgumentException.class, () ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://localhost:5432/trust_agro_real_pg_test?hostaddr=203.0.113.10"));
    }

    @Test
    void continuesToRejectUnexpectedDatabaseNamesOnLocalhost() {
        assertThrows(IllegalArgumentException.class, () ->
                RealPostgresVerificationTest.requireTestDatabase(
                        "jdbc:postgresql://localhost:5432/production"));
    }
}
