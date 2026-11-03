package com.trustagro.schema;

import com.trustagro.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.baseline-on-migrate=true",
    "spring.flyway.baseline-version=1",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "spring.datasource.url=jdbc:h2:mem:flyway_test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
class FlywayMigrationValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSeeder dataSeeder;

    @Test
    void flywayAppliesBaselineSchemaAndHibernateValidatesCleanly() {
        // Verify flyway_schema_history table exists and recorded V1
        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true", Integer.class);
        assertThat(historyCount).isNotNull();
        assertThat(historyCount).isEqualTo(1);

        // Verify key application tables created by Flyway exist
        Integer usersCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertThat(usersCount).isNotNull();

        Integer rolesCount = jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Integer.class);
        assertThat(rolesCount).isNotNull();
        assertThat(rolesCount).isGreaterThan(0); // Seeded by RoleSeeder

        assertV1CheckConstraintsSurviveStartup();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void legacyRolesAreNormalizedAndRepeatedSeedingIsIdempotent() {
        Map<String, String> legacyRoles = Map.of(
                "STORE_KEEPER", "INVENTORY_MANAGER",
                "PHARMACY_SALES", "PHARMACIST",
                "VETERINARY_OFFICER", "VETERINARIAN",
                "FINANCE_OFFICER", "FINANCE",
                "MARKETING", "MARKETING_MANAGER",
                "TRAINING_OFFICER", "TRAINING_COORDINATOR"
        );
        legacyRoles.forEach((legacyRole, canonicalRole) -> {
            jdbcTemplate.update(
                    "INSERT INTO users (email, full_name, password, role, status) VALUES (?, ?, ?, ?, ?)",
                    legacyRole.toLowerCase() + "@example.test", "Legacy " + legacyRole, "test-password",
                    legacyRole, "ACTIVE");
            jdbcTemplate.update(
                    "INSERT INTO notifications (title, target_role, type, is_read) VALUES (?, ?, ?, ?)",
                    "Legacy " + legacyRole, legacyRole, "INFO", false);
        });

        dataSeeder.run();
        assertLegacyRolesNormalized(legacyRoles);

        dataSeeder.run();
        assertLegacyRolesNormalized(legacyRoles);
    }

    private void assertLegacyRolesNormalized(Map<String, String> legacyRoles) {
        legacyRoles.forEach((legacyRole, canonicalRole) -> {
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT role FROM users WHERE email = ?",
                    String.class, legacyRole.toLowerCase() + "@example.test")).isEqualTo(canonicalRole);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT target_role FROM notifications WHERE title = ?",
                    String.class, "Legacy " + legacyRole)).isEqualTo(canonicalRole);
        });
    }

    private void assertV1CheckConstraintsSurviveStartup() {
        assertCheckConstraintExists("users", "role");
        assertCheckConstraintExists("users", "status");
        assertCheckConstraintExists("notifications", "target_role");
        assertCheckConstraintExists("notifications", "type");
    }

    private void assertCheckConstraintExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) " +
                        "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
                        "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc " +
                        "ON tc.CONSTRAINT_CATALOG = cc.CONSTRAINT_CATALOG " +
                        "AND tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA " +
                        "AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME " +
                        "WHERE LOWER(tc.TABLE_SCHEMA) = LOWER(CURRENT_SCHEMA()) " +
                        "AND LOWER(tc.TABLE_NAME) = LOWER(?) " +
                        "AND tc.CONSTRAINT_TYPE = 'CHECK' " +
                        "AND LOWER(cc.CHECK_CLAUSE) LIKE ?",
                Integer.class, table, "%" + column + "%");
        assertThat(count).as("CHECK constraint for %s.%s", table, column).isEqualTo(1);
    }
}
