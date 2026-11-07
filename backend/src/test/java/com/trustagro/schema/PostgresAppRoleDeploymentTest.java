package com.trustagro.schema;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the least-privilege application role created under the documented deployment procedure
 * (docker/postgres-init/10-create-app-role.sh and docs/production-deployment.md) can execute all Flyway
 * migrations (V1, V2, V3) and perform standard application operations (CRUD, sequences, transactions)
 * without requiring PostgreSQL superuser privileges.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgresAppRoleDeploymentTest {

    private static final String TEST_DB_NAME = "trust_agro_role_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String APP_USERNAME = "trust_agro_app_tester";
    private static final String APP_PASSWORD = "TestAppRolePass_AtLeast24CharsLong!";

    private static RealPostgresVerificationTest.PostgresConfiguration adminConfig;
    private static String appDbUrl;
    private static DriverManagerDataSource appDataSource;
    private static JdbcTemplate appJdbcTemplate;

    @BeforeAll
    static void setUpDatabaseAndRole() throws Exception {
        adminConfig = RealPostgresVerificationTest.getPostgresConfiguration();
        RealPostgresVerificationTest.createDatabase(adminConfig, TEST_DB_NAME);

        appDbUrl = RealPostgresVerificationTest.withDatabaseName(adminConfig.jdbcUrl(), TEST_DB_NAME);

        // Bootstrap script (equivalent to 10-create-app-role.sh) executed as postgres superuser
        String superuserTargetUrl = RealPostgresVerificationTest.withDatabaseName(adminConfig.jdbcUrl(), TEST_DB_NAME);
        try (Connection conn = DriverManager.getConnection(superuserTargetUrl, adminConfig.username(), adminConfig.password());
             Statement stmt = conn.createStatement()) {

            stmt.execute("DO $$ BEGIN "
                    + "IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + APP_USERNAME + "') THEN "
                    + "  CREATE ROLE " + APP_USERNAME + " LOGIN PASSWORD '" + APP_PASSWORD + "' NOSUPERUSER NOCREATEDB NOCREATEROLE; "
                    + "END IF; "
                    + "END $$;");
            stmt.execute("GRANT CONNECT ON DATABASE \"" + TEST_DB_NAME + "\" TO " + APP_USERNAME);
            stmt.execute("GRANT USAGE, CREATE ON SCHEMA public TO " + APP_USERNAME);
        }

        // Configure connection for least-privilege application role
        appDataSource = new DriverManagerDataSource();
        appDataSource.setDriverClassName("org.postgresql.Driver");
        appDataSource.setUrl(appDbUrl);
        appDataSource.setUsername(APP_USERNAME);
        appDataSource.setPassword(APP_PASSWORD);
        appJdbcTemplate = new JdbcTemplate(appDataSource);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (adminConfig != null) {
            try {
                RealPostgresVerificationTest.dropDatabase(adminConfig, TEST_DB_NAME);
            } catch (SQLException ignored) {
            }

            String adminUrl = RealPostgresVerificationTest.withDatabaseName(adminConfig.jdbcUrl(), "postgres");
            try (Connection conn = DriverManager.getConnection(adminUrl, adminConfig.username(), adminConfig.password());
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP ROLE IF EXISTS " + APP_USERNAME);
            } catch (SQLException ignored) {
            }
        }
        RealPostgresVerificationTest.stopLocalPostgres();
    }

    @Test
    @Order(1)
    void applicationRoleIsLeastPrivilegeNonSuperuser() {
        Boolean isSuperuser = appJdbcTemplate.queryForObject(
                "SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class);
        assertThat(isSuperuser).isFalse();

        Boolean canCreateDb = appJdbcTemplate.queryForObject(
                "SELECT rolcreatedb FROM pg_roles WHERE rolname = current_user", Boolean.class);
        assertThat(canCreateDb).isFalse();

        // Verifying least-privilege boundary: app role cannot create databases
        assertThatThrownBy(() -> appJdbcTemplate.execute("CREATE DATABASE unauthorized_db"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @Order(2)
    void applicationRoleCanExecuteFlywayMigrationsV1ThroughV4() {
        Flyway flyway = Flyway.configure()
                .dataSource(appDbUrl, APP_USERNAME, APP_PASSWORD)
                .locations("classpath:db/migration")
                .load();

        int migrationsApplied = flyway.migrate().migrationsExecuted;
        assertThat(migrationsApplied).isGreaterThanOrEqualTo(3);

        Integer historyCount = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(historyCount).isEqualTo(7);

        Integer v1Count = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true", Integer.class);
        assertThat(v1Count).isEqualTo(1);

        Integer v2Count = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success = true", Integer.class);
        assertThat(v2Count).isEqualTo(1);

        Integer v3Count = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success = true", Integer.class);
        assertThat(v3Count).isEqualTo(1);

        Integer v4Count = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '4' AND success = true", Integer.class);
        assertThat(v4Count).isEqualTo(1);

        Integer v5Count = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '5' AND success = true", Integer.class);
        assertThat(v5Count).isEqualTo(1);

        // Verify token_version column exists on users table (V3)
        Integer tokenVersionColCount = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' "
                        + "AND table_name = 'users' AND column_name = 'token_version'", Integer.class);
        assertThat(tokenVersionColCount).isEqualTo(1);
    }

    @Test
    @Order(3)
    void applicationRoleCanPerformNormalCrudAndInventoryReceiptTransactions() {
        // 1. User creation with token_version persistence
        appJdbcTemplate.update(
                "INSERT INTO users (email, full_name, password, role, status, token_version) "
                        + "VALUES ('appuser@trustagro.test', 'App Role User', '$2a$10$xyz', 'FARM_WORKER', 'ACTIVE', 0)");

        Long userId = appJdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'appuser@trustagro.test'", Long.class);
        assertThat(userId).isNotNull();

        // 2. Token version update
        appJdbcTemplate.update("UPDATE users SET token_version = token_version + 1 WHERE id = ?", userId);
        Long updatedVersion = appJdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE id = ?", Long.class, userId);
        assertThat(updatedVersion).isEqualTo(1L);

        // 3. Chart of Accounts setup
        appJdbcTemplate.update(
                "INSERT INTO chart_of_accounts (account_code, account_name, account_type, business_unit, balance, is_header) "
                        + "VALUES ('1001_INV', 'Inventory Asset', 'ASSET', 'GENERAL', 0.00, false)");
        appJdbcTemplate.update(
                "INSERT INTO chart_of_accounts (account_code, account_name, account_type, business_unit, balance, is_header) "
                        + "VALUES ('2001_AP', 'Accounts Payable', 'LIABILITY', 'GENERAL', 0.00, false)");

        Long invAccountId = appJdbcTemplate.queryForObject(
                "SELECT id FROM chart_of_accounts WHERE account_code = '1001_INV'", Long.class);
        Long apAccountId = appJdbcTemplate.queryForObject(
                "SELECT id FROM chart_of_accounts WHERE account_code = '2001_AP'", Long.class);

        // 4. Paired finance transaction (V2 check: distinct accounts, positive amount, non-null)
        appJdbcTemplate.update(
                "INSERT INTO finance_transactions "
                        + "(transaction_type, amount, debit_account_id, credit_account_id, business_unit, "
                        + "description, source_module, reference_type, reference_id) "
                        + "VALUES ('TRANSFER', 150.00, ?, ?, 'GENERAL', 'Stock receipt: Feed', 'INVENTORY', 'STOCK_IN', 101)",
                invAccountId, apAccountId);

        Integer txCount = appJdbcTemplate.queryForObject(
                "SELECT count(*) FROM finance_transactions WHERE reference_type = 'STOCK_IN'", Integer.class);
        assertThat(txCount).isEqualTo(1);

        // 5. Inventory item and stock receipt movement
        appJdbcTemplate.update(
                "INSERT INTO inventory_items (sku, item_name, category, unit, current_stock) "
                        + "VALUES ('SKU-FEED-01', 'Broiler Starter Feed', 'FEED', 'KG', 50.0)");
        Long itemId = appJdbcTemplate.queryForObject(
                "SELECT id FROM inventory_items WHERE sku = 'SKU-FEED-01'", Long.class);

        appJdbcTemplate.update(
                "INSERT INTO inventory_batches (batch_number, inventory_item_id, quantity_received, quantity_remaining, received_date, is_active) "
                        + "VALUES ('BATCH-2026-001', ?, 50.0, 50.0, CURRENT_DATE, true)", itemId);
        Long batchId = appJdbcTemplate.queryForObject(
                "SELECT id FROM inventory_batches WHERE batch_number = 'BATCH-2026-001'", Long.class);

        appJdbcTemplate.update(
                "INSERT INTO stock_movements (inventory_item_id, batch_id, movement_type, quantity, reference_id) "
                        + "VALUES (?, ?, 'RECEIPT', 50.0, 101)", itemId, batchId);

        Double totalStock = appJdbcTemplate.queryForObject(
                "SELECT current_stock FROM inventory_items WHERE id = ?", Double.class, itemId);
        assertThat(totalStock).isEqualTo(50.0);
    }
}
