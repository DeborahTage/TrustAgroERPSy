package com.trustagro.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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

    @Test
    void flywayAppliesBaselineSchemaAndHibernateValidatesCleanly() {
        // Verify flyway_schema_history table exists and recorded V1
        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1'", Integer.class);
        assertThat(historyCount).isNotNull();
        assertThat(historyCount).isEqualTo(1);

        // Verify key application tables created by Flyway exist
        Integer usersCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertThat(usersCount).isNotNull();

        Integer rolesCount = jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Integer.class);
        assertThat(rolesCount).isNotNull();
        assertThat(rolesCount).isGreaterThan(0); // Seeded by RoleSeeder
    }
}
