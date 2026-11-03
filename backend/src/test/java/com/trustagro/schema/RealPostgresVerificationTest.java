package com.trustagro.schema;

import com.trustagro.config.InitialAdminProvisioner;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.RoleRepository;
import com.trustagro.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RealPostgresVerificationTest {

    private static final String JDBC_URL_ENV = "REAL_POSTGRES_JDBC_URL";
    private static final String USERNAME_ENV = "REAL_POSTGRES_USERNAME";
    private static final String PASSWORD_ENV = "REAL_POSTGRES_PASSWORD";
    private static final String DATABASE_NAME = "trust_agro_real_pg_test";

    private static PostgresConfiguration postgresConfiguration;
    private static Path localTempDirectory;
    private static Path localDataDirectory;
    private static Path localPgCtl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        PostgresConfiguration configuration = getPostgresConfiguration();
        registry.add("spring.datasource.url", configuration::jdbcUrl);
        registry.add("spring.datasource.username", configuration::username);
        registry.add("spring.datasource.password", configuration::password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("app.jwt.secret", () -> "RealPostgresTestJwtSecretKeyMustBe32CharsLong");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
        registry.add("INITIAL_ADMIN_EMAIL", () -> "admin@trustagro.com");
        registry.add("INITIAL_ADMIN_PASSWORD", () -> "TestAdminPass123!");
        registry.add("INITIAL_ADMIN_NAME", () -> "Trust Agro Super Admin");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private InitialAdminProvisioner initialAdminProvisioner;

    @Test
    @Order(1)
    void flywayAppliesBaselineAndHibernateValidatesCleanlyOnRealPostgres() {
        Integer history = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true", Integer.class);
        assertThat(history).isEqualTo(1);

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name != 'flyway_schema_history'",
                Integer.class);
        assertThat(tableCount).isEqualTo(130);

        assertCheckConstraintExists("users", "role");
        assertCheckConstraintExists("users", "status");
        assertCheckConstraintExists("notifications", "target_role");
        assertCheckConstraintExists("notifications", "type");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(2)
    void initialAdminProvisionedSecurelyWithBcrypt() {
        User admin = userRepository.findByEmail("admin@trustagro.com").orElse(null);
        assertThat(admin).isNotNull();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getFullName()).isEqualTo("Trust Agro Super Admin");
        assertThat(passwordEncoder.matches("TestAdminPass123!", admin.getPassword())).isTrue();
        assertThat(admin.getPassword()).startsWith("$2a$");
    }

    @Test
    @Order(3)
    void restartDoesNotDuplicateOrOverwriteAdmin() {
        long initialCount = userRepository.count();
        initialAdminProvisioner.run();

        assertThat(userRepository.count()).isEqualTo(initialCount);
        User admin = userRepository.findByEmail("admin@trustagro.com").orElse(null);
        assertThat(admin).isNotNull();
        assertThat(passwordEncoder.matches("TestAdminPass123!", admin.getPassword())).isTrue();
    }

    @Test
    @Order(4)
    void roleSeederPopulatedCanonicalRolesInPostgres() {
        long roleCount = roleRepository.count();
        assertThat(roleCount).isGreaterThan(10);
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
        assertThat(roleRepository.findByName("FARM_MANAGER")).isPresent();
        assertThat(roleRepository.findByName("FINANCE")).isPresent();
    }

    @Test
    @Order(5)
    void uniqueConstraintPreventsDuplicateUsers() {
        User duplicate = new User();
        duplicate.setEmail("admin@trustagro.com");
        duplicate.setFullName("Duplicate User");
        duplicate.setPassword("pass");
        duplicate.setRole(Role.FARM_WORKER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Order(6)
    void foreignKeyConstraintPreventsOrphanChildren() {
        assertThatThrownBy(() -> jdbcTemplate.execute(
                "INSERT INTO barns (id, name, farm_id) VALUES (99999, 'Orphan Barn', 9999999)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Order(7)
    void transactionRollbackPreservesIntegrity() {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tt.execute(status -> {
            jdbcTemplate.update("INSERT INTO hr_departments (id, code, name, active) VALUES (88888, 'TEMP_DEPT', 'Temporary', true)");
            throw new RuntimeException("Simulated failure inside transaction");
        })).hasMessageContaining("Simulated failure inside transaction");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM hr_departments WHERE code = 'TEMP_DEPT'", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    private void assertCheckConstraintExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) " +
                        "FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.check_constraints cc " +
                        "ON tc.constraint_catalog = cc.constraint_catalog " +
                        "AND tc.constraint_schema = cc.constraint_schema " +
                        "AND tc.constraint_name = cc.constraint_name " +
                        "WHERE lower(tc.table_schema) = lower(current_schema()) " +
                        "AND lower(tc.table_name) = lower(?) " +
                        "AND tc.constraint_type = 'CHECK' " +
                        "AND lower(cc.check_clause) LIKE ?",
                Integer.class, table, "%" + column + "%");
        assertThat(count).as("CHECK constraint for %s.%s", table, column).isEqualTo(1);
    }

    @Test
    @Order(8)
    void existingDatabaseWithPreExistingTablesBaselinesSafelyWithoutDataLoss() throws Exception {
        String existingDatabaseName = "trust_agro_existing_test_" + UUID.randomUUID().toString().replace("-", "");
        String existingDbUrl = withDatabaseName(postgresConfiguration.jdbcUrl(), existingDatabaseName);
        createDatabase(postgresConfiguration, existingDatabaseName);

        try {
            org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(existingDbUrl);
            ds.setUsername(postgresConfiguration.username());
            ds.setPassword(postgresConfiguration.password());
            JdbcTemplate existingJdbc = new JdbcTemplate(ds);

            existingJdbc.execute("CREATE TABLE pre_existing_users (id bigserial primary key, email varchar(255), name varchar(255))");
            existingJdbc.execute("INSERT INTO pre_existing_users (email, name) VALUES ('veteran@trustagro.com', 'Veteran User')");

            Flyway existingFlyway = Flyway.configure()
                    .dataSource(existingDbUrl, postgresConfiguration.username(), postgresConfiguration.password())
                    .baselineOnMigrate(true)
                    .baselineVersion("1")
                    .locations("classpath:db/migration")
                    .load();

            existingFlyway.migrate();

            String baselineType = existingJdbc.queryForObject(
                    "SELECT type FROM flyway_schema_history WHERE version = '1'", String.class);
            assertThat(baselineType).isEqualTo("BASELINE");

            String veteranName = existingJdbc.queryForObject(
                    "SELECT name FROM pre_existing_users WHERE email = 'veteran@trustagro.com'", String.class);
            assertThat(veteranName).isEqualTo("Veteran User");
        } finally {
            dropDatabase(postgresConfiguration, existingDatabaseName);
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void stopLocalPostgres() throws Exception {
        if (localTempDirectory == null) {
            return;
        }

        if (localDataDirectory != null && Files.exists(localDataDirectory.resolve("postmaster.pid"))) {
            runCommand(localPgCtl.toString(), "-D", localDataDirectory.toString(), "-m", "fast", "-w", "stop");
        }

        try (var paths = Files.walk(localTempDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static synchronized PostgresConfiguration getPostgresConfiguration() {
        if (postgresConfiguration != null) {
            return postgresConfiguration;
        }

        String jdbcUrl = System.getenv(JDBC_URL_ENV);
        String username = System.getenv(USERNAME_ENV);
        String password = System.getenv(PASSWORD_ENV);
        boolean externalConfigurationProvided = jdbcUrl != null || username != null || password != null;

        try {
            if (externalConfigurationProvided) {
                if (jdbcUrl == null || jdbcUrl.isBlank() || username == null || username.isBlank() || password == null) {
                    throw new IllegalStateException(
                            JDBC_URL_ENV + ", " + USERNAME_ENV + ", and " + PASSWORD_ENV + " must be configured together");
                }
                requireTestDatabase(jdbcUrl);
                postgresConfiguration = new PostgresConfiguration(jdbcUrl, username, password);
            } else {
                postgresConfiguration = startLocalPostgres();
            }
            return postgresConfiguration;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start disposable local PostgreSQL for integration tests", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create the disposable local PostgreSQL test database", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting disposable local PostgreSQL", exception);
        }
    }

    private static PostgresConfiguration startLocalPostgres() throws IOException, InterruptedException, SQLException {
        Path initdb = findPostgresExecutable("initdb");
        localPgCtl = findPostgresExecutable("pg_ctl");
        localTempDirectory = Files.createTempDirectory("trust-agro-real-postgres-");
        localDataDirectory = localTempDirectory.resolve("data");
        Path dataDirectory = localDataDirectory;
        Path socketDirectory = localTempDirectory.resolve("socket");
        Files.createDirectory(socketDirectory);
        Path logFile = localTempDirectory.resolve("postgres.log");
        int port = findAvailablePort();

        runCommand(initdb.toString(), "-D", dataDirectory.toString(), "-U", "postgres",
                "--auth-local=trust", "--auth-host=trust", "--no-locale", "-E", "UTF8");
        try {
            runCommand(localPgCtl.toString(), "-D", dataDirectory.toString(), "-o",
                    "-h 127.0.0.1 -p " + port + " -k " + socketDirectory,
                    "-l", logFile.toString(), "-w", "start");
        } catch (IOException exception) {
            if (Files.exists(logFile)) {
                exception.addSuppressed(new IOException("PostgreSQL server log:\n" + Files.readString(logFile)));
            }
            throw exception;
        }

        String jdbcUrl = "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
        PostgresConfiguration localConfiguration = new PostgresConfiguration(jdbcUrl, "postgres", "");
        createDatabase(localConfiguration, DATABASE_NAME);
        return new PostgresConfiguration(withDatabaseName(jdbcUrl, DATABASE_NAME), "postgres", "");
    }

    private static Path findPostgresExecutable(String executable) throws IOException {
        String configuredBinDirectory = System.getenv("REAL_POSTGRES_BIN_DIR");
        if (configuredBinDirectory != null && !configuredBinDirectory.isBlank()) {
            Path candidate = Path.of(configuredBinDirectory).resolve(executable);
            if (Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath();
            }
            throw new IOException("REAL_POSTGRES_BIN_DIR does not contain executable " + executable);
        }

        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(java.io.File.pathSeparator)) {
                Path candidate = Path.of(directory).resolve(executable);
                if (Files.isExecutable(candidate)) {
                    return candidate.toAbsolutePath();
                }
            }
        }
        throw new IOException("Could not find " + executable
                + "; install PostgreSQL, add it to PATH, or set REAL_POSTGRES_BIN_DIR");
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (" + exitCode + "): " + command[0] + "\n" + output);
        }
    }

    private static void createDatabase(PostgresConfiguration configuration, String databaseName) throws SQLException {
        String adminUrl = withDatabaseName(configuration.jdbcUrl(), "postgres");
        try (Connection connection = DriverManager.getConnection(
                adminUrl, configuration.username(), configuration.password());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE \"" + databaseName + "\"");
        }
    }

    private static void dropDatabase(PostgresConfiguration configuration, String databaseName) throws SQLException {
        String adminUrl = withDatabaseName(configuration.jdbcUrl(), "postgres");
        try (Connection connection = DriverManager.getConnection(
                adminUrl, configuration.username(), configuration.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE \"" + databaseName + "\"");
        }
    }

    private static String withDatabaseName(String jdbcUrl, String databaseName) {
        try {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            if (!"postgresql".equals(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("PostgreSQL JDBC URL must include a host and database name");
            }
            URI updated = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    "/" + databaseName, uri.getQuery(), null);
            return "jdbc:" + updated;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid PostgreSQL JDBC URL", exception);
        }
    }

    static void requireTestDatabase(String jdbcUrl) {
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        if (!"postgresql".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Real PostgreSQL verification requires a PostgreSQL JDBC URL");
        }

        String host = uri.getHost();
        if (!"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
            throw new IllegalArgumentException(
                    "Real PostgreSQL verification requires a localhost or loopback database host");
        }
        if (hasHostOverride(uri.getRawQuery())) {
            throw new IllegalArgumentException(
                    "Real PostgreSQL verification does not allow JDBC host or hostaddr overrides");
        }

        String databaseName = uri.getPath();
        if (databaseName == null
                || !"/trust_agro_real_pg_test".equals(databaseName.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Real PostgreSQL verification requires a dedicated test database");
        }
    }

    private static boolean hasHostOverride(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }

        for (String parameter : query.split("&")) {
            String key = parameter.split("=", 2)[0];
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            if ("host".equalsIgnoreCase(decodedKey) || "hostaddr".equalsIgnoreCase(decodedKey)) {
                return true;
            }
        }
        return false;
    }

    private record PostgresConfiguration(String jdbcUrl, String username, String password) {
    }

}
