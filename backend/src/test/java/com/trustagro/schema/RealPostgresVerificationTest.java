package com.trustagro.schema;

import com.trustagro.config.InitialAdminProvisioner;
import com.trustagro.common.exception.FinancePostingConflictException;
import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.service.FinancePostingRequest;
import com.trustagro.finance.service.FinanceService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.ServerSocket;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
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

    @Autowired
    private FinanceService financeService;

    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;

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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' "
                        + "AND table_name = 'users' AND column_name = 'token_version'",
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

    @Test
    @Order(9)
    void concurrentDebitsAgainstOneAccountDoNotLoseUpdates() {
        try (TestAccounts accounts = createTestAccounts()) {
            int postingCount = 12;
            runConcurrently(postingCount, index -> post(
                    accounts.accountA, accounts.accountB, accounts.module("DEBIT"), index + 1L));

            assertFinancePostingsConsistent(accounts, postingCount);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("12.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-12.00");
        }
    }

    @Test
    @Order(10)
    void concurrentCreditsAgainstOneAccountDoNotLoseUpdates() {
        try (TestAccounts accounts = createTestAccounts()) {
            int postingCount = 12;
            runConcurrently(postingCount, index -> post(
                    accounts.accountA, accounts.accountB, accounts.module("CREDIT"), index + 1L));

            assertFinancePostingsConsistent(accounts, postingCount);
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-12.00");
        }
    }

    @Test
    @Order(11)
    void concurrentDebitAndCreditActivityOnOneAccountDoesNotLoseUpdates() {
        try (TestAccounts accounts = createTestAccounts()) {
            int postingCount = 12;
            runConcurrently(postingCount, index -> index % 2 == 0
                    ? post(accounts.accountA, accounts.accountB, accounts.module("MIXED"), index + 1L)
                    : post(accounts.accountC, accounts.accountA, accounts.module("MIXED"), index + 1L));

            assertFinancePostingsConsistent(accounts, postingCount);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-6.00");
            assertThat(balance(accounts.accountC)).isEqualByComparingTo("6.00");
        }
    }

    @Test
    @Order(12)
    void oppositeAccountPairOrderUsesConsistentLockOrder() {
        try (TestAccounts accounts = createTestAccounts()) {
            int postingCount = 12;
            runConcurrently(postingCount, index -> index % 2 == 0
                    ? post(accounts.accountA, accounts.accountB, accounts.module("OPPOSITE"), index + 1L)
                    : post(accounts.accountB, accounts.accountA, accounts.module("OPPOSITE"), index + 1L));

            assertFinancePostingsConsistent(accounts, postingCount);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("0.00");
        }
    }

    @Test
    @Order(13)
    void simultaneousPostingsFromDifferentSourcesPreserveBalances() {
        try (TestAccounts accounts = createTestAccounts()) {
            int postingCount = 12;
            runConcurrently(postingCount, index -> post(
                    accounts.accountA, accounts.accountB, accounts.module("SOURCE_" + index), 1L));

            assertFinancePostingsConsistent(accounts, postingCount);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("12.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-12.00");
        }
    }

    @Test
    @Order(14)
    void concurrentDuplicateSourceReferenceAppliesBalanceOnlyOnce() {
        try (TestAccounts accounts = createTestAccounts()) {
            List<Long> transactionIds = runConcurrently(2, index -> post(
                    accounts.accountA, accounts.accountB, accounts.module("DUPLICATE"), 1L));

            assertThat(transactionIds).containsOnly(transactionIds.get(0));
            assertFinancePostingsConsistent(accounts, 1);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("1.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-1.00");
        }
    }

    @Test
    @Order(15)
    void sequentialConflictingDuplicateReferenceIsRejectedWithoutChangingBalances() {
        try (TestAccounts accounts = createTestAccounts()) {
            String module = accounts.module("CONFLICT");
            FinanceTransaction original = post(accounts.accountA, accounts.accountB, module, 1L);
            assertThatThrownBy(() -> financeService.postTransaction(TransactionType.TRANSFER,
                    new BigDecimal("2.00"), BusinessUnit.GENERAL,
                    accounts.accountA, accounts.accountB, "Conflicting retry",
                    module, "M2_POSTING", 1L))
                    .isInstanceOf(FinancePostingConflictException.class);

            assertThat(balance(accounts.accountA)).isEqualByComparingTo("1.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-1.00");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM finance_transactions WHERE source_module = ?",
                    Integer.class, module)).isEqualTo(1);
            assertThat(original.getId()).isNotNull();
            assertFinancePostingsConsistent(accounts, 1);
        }
    }

    @Test
    @Order(16)
    void concurrentConflictingDuplicateReferenceWithDifferentAccountsReturnsConflict() {
        try (TestAccounts accounts = createTestAccounts()) {
            String module = accounts.module("RACE_CONFLICT");
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            TransactionTemplate independentTransaction = new TransactionTemplate(transactionManager);
            independentTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            List<Future<Object>> futures = new ArrayList<>();

            try {
                futures.add(executor.submit(() -> attemptConcurrentPosting(
                        ready, start, independentTransaction, accounts.accountA, accounts.accountB, module)));
                futures.add(executor.submit(() -> attemptConcurrentPosting(
                        ready, start, independentTransaction, accounts.accountC, accounts.accountD, module)));
                assertThat(ready.await(20, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                List<Object> outcomes = new ArrayList<>();
                for (Future<Object> future : futures) {
                    outcomes.add(future.get(30, TimeUnit.SECONDS));
                }
                assertThat(outcomes).filteredOn(FinanceTransaction.class::isInstance).hasSize(1);
                assertThat(outcomes).filteredOn(
                        FinancePostingConflictException.class::isInstance).hasSize(1);
                assertFinancePostingsConsistent(accounts, 1);
                boolean firstPairWon = balance(accounts.accountA).compareTo(BigDecimal.ONE) == 0
                        && balance(accounts.accountB).compareTo(BigDecimal.ONE.negate()) == 0;
                boolean secondPairWon = balance(accounts.accountC).compareTo(BigDecimal.ONE) == 0
                        && balance(accounts.accountD).compareTo(BigDecimal.ONE.negate()) == 0;
                assertThat(firstPairWon || secondPairWon).isTrue();
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new AssertionError("Concurrent conflicting finance posting did not resolve cleanly", exception);
            } finally {
                start.countDown();
                futures.forEach(future -> future.cancel(true));
                executor.shutdownNow();
                try {
                    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted while stopping conflicting posting workers", exception);
                }
            }
        }
    }

    @Test
    @Order(17)
    void reversedMultiPostingOuterTransactionsLockTheirAccountUnionBeforePosting() {
        try (TestAccounts accounts = createTestAccounts()) {
            List<List<Long>> transactionIds = runConcurrentBatches(2, index -> {
                String module = accounts.module("MULTI_" + index);
                List<FinancePostingRequest> requests = List.of(
                        posting(accounts.accountA, accounts.accountB, module, "ENTRY_ONE", 1L),
                        posting(accounts.accountC, accounts.accountD, module, "ENTRY_TWO", 2L));
                if (index == 1) {
                    requests = List.of(requests.get(1), requests.get(0));
                }
                return financeService.postTransactions(requests).stream()
                        .map(FinanceTransaction::getId).toList();
            });

            assertThat(transactionIds).allSatisfy(ids -> assertThat(ids).hasSize(2));
            assertFinancePostingsConsistent(accounts, 4);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("2.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-2.00");
            assertThat(balance(accounts.accountC)).isEqualByComparingTo("2.00");
            assertThat(balance(accounts.accountD)).isEqualByComparingTo("-2.00");
        }
    }

    @Test
    @Order(18)
    void persistenceFailureRollsBackBothBalancesAndLedgerRecords() {
        try (TestAccounts accounts = createTestAccounts()) {
            String module = accounts.module("ROLLBACK");
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

            assertThatThrownBy(() -> transaction.execute(status -> {
                financeService.postTransactions(List.of(
                        posting(accounts.accountA, accounts.accountB, module, "M2_ROLLBACK", 1L),
                        posting(accounts.accountC, accounts.accountD, module, "M2_ROLLBACK_SECOND", 2L)));
                jdbcTemplate.update("INSERT INTO finance_transactions "
                                + "(transaction_type, amount, debit_account_id, credit_account_id) "
                                + "VALUES ('INCOME', 1.00, NULL, NULL)");
                return null;
            })).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(balance(accounts.accountA)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountC)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountD)).isEqualByComparingTo("0.00");
            assertFinancePostingsConsistent(accounts, 0);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM audit_logs WHERE module = 'FINANCE' "
                            + "AND entity_type = 'FinanceTransaction' AND new_value LIKE ?",
                    Integer.class, "%source=" + module + "%")).isZero();
        }
    }

    @Test
    @Order(19)
    void mixedBatchReturnsIdenticalExistingPostingAndPostsNewEntryOnce() {
        try (TestAccounts accounts = createTestAccounts()) {
            String module = accounts.module("MIXED_RETRY");
            FinanceTransaction existing = post(accounts.accountA, accounts.accountB, module, 1L);
            List<FinancePostingRequest> batch = List.of(
                    posting(accounts.accountA, accounts.accountB, module, "M2_POSTING", 1L),
                    posting(accounts.accountC, accounts.accountD, module, "NEW_ENTRY", 2L));

            List<FinanceTransaction> firstResult = financeService.postTransactions(batch);
            assertThat(firstResult).hasSize(2);
            assertThat(firstResult.get(0).getId()).isEqualTo(existing.getId());
            assertThat(firstResult.get(0).getDescription()).isEqualTo(existing.getDescription());
            assertThat(firstResult.get(1).getId()).isNotEqualTo(existing.getId());

            List<FinanceTransaction> retryResult = financeService.postTransactions(batch);
            assertThat(retryResult).extracting(FinanceTransaction::getId)
                    .containsExactlyElementsOf(firstResult.stream().map(FinanceTransaction::getId).toList());
            assertThat(retryResult.get(0).getDescription()).isEqualTo(existing.getDescription());
            assertFinancePostingsConsistent(accounts, 2);
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("1.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-1.00");
            assertThat(balance(accounts.accountC)).isEqualByComparingTo("1.00");
            assertThat(balance(accounts.accountD)).isEqualByComparingTo("-1.00");
        }
    }

    @Test
    @Order(20)
    void conflictingReferenceRejectsWholeBatchWithoutNewLedgerOrAuditWrites() {
        try (TestAccounts accounts = createTestAccounts()) {
            String module = accounts.module("MIXED_CONFLICT");
            FinanceTransaction existing = post(accounts.accountA, accounts.accountB, module, 1L);
            List<FinancePostingRequest> batch = List.of(
                    posting(accounts.accountC, accounts.accountD, module, "NEW_ENTRY", 2L),
                    FinancePostingRequest.of(TransactionType.TRANSFER, new BigDecimal("2.00"),
                            BusinessUnit.GENERAL, accounts.accountA, accounts.accountB,
                            "Conflicting retry", module, "M2_POSTING", 1L));

            assertThatThrownBy(() -> financeService.postTransactions(batch))
                    .isInstanceOf(FinancePostingConflictException.class);

            assertThat(existing.getId()).isNotNull();
            assertThat(balance(accounts.accountA)).isEqualByComparingTo("1.00");
            assertThat(balance(accounts.accountB)).isEqualByComparingTo("-1.00");
            assertThat(balance(accounts.accountC)).isEqualByComparingTo("0.00");
            assertThat(balance(accounts.accountD)).isEqualByComparingTo("0.00");
            assertFinancePostingsConsistent(accounts, 1);
        }
    }

    private Object attemptConcurrentPosting(CountDownLatch ready, CountDownLatch start,
                                            TransactionTemplate transaction, String debitAccount,
                                            String creditAccount, String module) throws Exception {
        ready.countDown();
        if (!start.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start duplicate finance postings");
        }
        try {
            return transaction.execute(status -> post(debitAccount, creditAccount, module, 1L));
        } catch (FinancePostingConflictException conflict) {
            return conflict;
        }
    }

    private FinancePostingRequest posting(String debitAccount, String creditAccount,
                                          String sourceModule, String referenceType, long referenceId) {
        return FinancePostingRequest.of(TransactionType.TRANSFER, BigDecimal.ONE,
                BusinessUnit.GENERAL, debitAccount, creditAccount, "Multi-posting test",
                sourceModule, referenceType, referenceId);
    }

    private List<List<Long>> runConcurrentBatches(int taskCount, IntFunction<List<Long>> posting) {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        TransactionTemplate independentTransaction = new TransactionTemplate(transactionManager);
        independentTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        List<Future<List<Long>>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                int postingIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start multi-posting transactions");
                    }
                    return independentTransaction.execute(status -> posting.apply(postingIndex));
                }));
            }
            assertThat(ready.await(20, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<List<Long>> results = new ArrayList<>();
            for (Future<List<Long>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AssertionError("Concurrent multi-posting transaction failed", exception);
        } finally {
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Concurrent multi-posting workers did not terminate");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while stopping multi-posting workers", exception);
            }
        }
    }

    private FinanceTransaction post(String debitAccount, String creditAccount, String sourceModule, long referenceId) {
        return financeService.postTransaction(TransactionType.TRANSFER, new BigDecimal("1.00"),
                BusinessUnit.GENERAL, debitAccount, creditAccount, "Concurrency integration test",
                sourceModule, "M2_POSTING", referenceId);
    }

    private List<Long> runConcurrently(int taskCount, IntFunction<FinanceTransaction> posting) {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        TransactionTemplate independentTransaction = new TransactionTemplate(transactionManager);
        independentTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        List<Future<Long>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                int postingIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent finance postings");
                    }
                    return independentTransaction.execute(status -> posting.apply(postingIndex).getId());
                }));
            }

            assertThat(ready.await(20, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Long> transactionIds = new ArrayList<>();
            for (Future<Long> future : futures) {
                transactionIds.add(future.get(30, TimeUnit.SECONDS));
            }
            return transactionIds;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AssertionError("Concurrent PostgreSQL finance posting failed", exception);
        } finally {
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Concurrent PostgreSQL posting workers did not terminate");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while stopping concurrent posting workers", exception);
            }
        }
    }

    private TestAccounts createTestAccounts() {
        String prefix = "M2CONC" + UUID.randomUUID().toString().replace("-", "");
        String accountA = prefix + "A";
        String accountB = prefix + "B";
        String accountC = prefix + "C";
        String accountD = prefix + "D";
        chartOfAccountRepository.saveAndFlush(testAccount(accountA));
        chartOfAccountRepository.saveAndFlush(testAccount(accountB));
        chartOfAccountRepository.saveAndFlush(testAccount(accountC));
        chartOfAccountRepository.saveAndFlush(testAccount(accountD));
        return new TestAccounts(prefix, accountA, accountB, accountC, accountD);
    }

    private ChartOfAccount testAccount(String accountCode) {
        ChartOfAccount account = new ChartOfAccount();
        account.setAccountCode(accountCode);
        account.setAccountName("Concurrency test " + accountCode);
        account.setAccountType(AccountType.ASSET);
        account.setBusinessUnit(BusinessUnit.GENERAL);
        account.setBalance(BigDecimal.ZERO.setScale(2));
        return account;
    }

    private BigDecimal balance(String accountCode) {
        return chartOfAccountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new AssertionError("Missing test account " + accountCode))
                .getBalance();
    }

    private void assertFinancePostingsConsistent(TestAccounts accounts, int expectedCount) {
        Integer transactionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM finance_transactions WHERE source_module LIKE ?",
                Integer.class, accounts.prefix + "%");
        Integer financeAuditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM finance_audit_logs WHERE source_module LIKE ?",
                Integer.class, accounts.prefix + "%");
        Integer generalAuditCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE module = 'FINANCE' "
                        + "AND entity_type = 'FinanceTransaction' "
                        + "AND entity_id IN (SELECT id FROM finance_transactions WHERE source_module LIKE ?)",
                Integer.class, accounts.prefix + "%");
        assertThat(transactionCount).isEqualTo(expectedCount);
        assertThat(financeAuditCount).isEqualTo(expectedCount);
        assertThat(generalAuditCount).isEqualTo(expectedCount);
        assertThat(ledgerBalance(accounts.accountA, accounts.prefix))
                .isEqualByComparingTo(balance(accounts.accountA));
        assertThat(ledgerBalance(accounts.accountB, accounts.prefix))
                .isEqualByComparingTo(balance(accounts.accountB));
        assertThat(ledgerBalance(accounts.accountC, accounts.prefix))
                .isEqualByComparingTo(balance(accounts.accountC));
        assertThat(ledgerBalance(accounts.accountD, accounts.prefix))
                .isEqualByComparingTo(balance(accounts.accountD));
    }

    private BigDecimal ledgerBalance(String accountCode, String sourcePrefix) {
        Long accountId = chartOfAccountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new AssertionError("Missing test account " + accountCode))
                .getId();
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN debit_account_id = ? THEN amount "
                        + "WHEN credit_account_id = ? THEN -amount ELSE 0 END), 0) "
                        + "FROM finance_transactions WHERE source_module LIKE ?",
                BigDecimal.class, accountId, accountId, sourcePrefix + "%");
    }

    private void cleanupTestData(TestAccounts accounts) {
        String sourcePattern = accounts.prefix + "%";
        jdbcTemplate.update("DELETE FROM audit_logs WHERE entity_type = 'FinanceTransaction' "
                        + "AND entity_id IN (SELECT id FROM finance_transactions WHERE source_module LIKE ?)",
                sourcePattern);
        jdbcTemplate.update("DELETE FROM finance_audit_logs WHERE source_module LIKE ?", sourcePattern);
        jdbcTemplate.update("DELETE FROM finance_transactions WHERE source_module LIKE ?", sourcePattern);
        jdbcTemplate.update("DELETE FROM chart_of_accounts WHERE account_code LIKE ?", sourcePattern);
    }

    private final class TestAccounts implements AutoCloseable {
        private final String prefix;
        private final String accountA;
        private final String accountB;
        private final String accountC;
        private final String accountD;

        private TestAccounts(String prefix, String accountA, String accountB, String accountC, String accountD) {
            this.prefix = prefix;
            this.accountA = accountA;
            this.accountB = accountB;
            this.accountC = accountC;
            this.accountD = accountD;
        }

        private String module(String suffix) {
            return prefix + suffix;
        }

        @Override
        public void close() {
            cleanupTestData(this);
        }
    }

    private void assertCheckConstraintExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) " +
                        "FROM pg_constraint c " +
                        "JOIN pg_class t ON t.oid = c.conrelid " +
                        "JOIN pg_namespace n ON n.oid = t.relnamespace " +
                        "WHERE n.nspname = current_schema() " +
                        "AND lower(t.relname) = lower(?) " +
                        "AND c.contype = 'c' " +
                        "AND pg_get_constraintdef(c.oid) ILIKE ?",
                Integer.class, table, "%" + column + "%");
        assertThat(count).as("CHECK constraint for %s.%s", table, column).isGreaterThan(0);
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

            Flyway initialSchema = Flyway.configure()
                    .dataSource(existingDbUrl, postgresConfiguration.username(), postgresConfiguration.password())
                    .locations("classpath:db/migration")
                    .target("1")
                    .load();
            initialSchema.migrate();

            existingJdbc.execute("CREATE TABLE pre_existing_users (id bigserial primary key, email varchar(255), name varchar(255))");
            existingJdbc.execute("INSERT INTO pre_existing_users (email, name) VALUES ('veteran@trustagro.com', 'Veteran User')");
            existingJdbc.execute("DROP TABLE flyway_schema_history");

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
            assertThat(existingJdbc.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success = true",
                    Integer.class)).isEqualTo(1);

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

        try {
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
        } finally {
            postgresConfiguration = null;
            localTempDirectory = null;
            localDataDirectory = null;
            localPgCtl = null;
        }
    }

    static synchronized PostgresConfiguration getPostgresConfiguration() {
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

        List<Path> standardDirectories = List.of(
                Path.of(System.getProperty("user.home"), "tools/pgsql/usr/lib/postgresql/14/bin"),
                Path.of("/usr/lib/postgresql/14/bin"),
                Path.of("/usr/lib/postgresql/15/bin"),
                Path.of("/usr/lib/postgresql/16/bin"),
                Path.of("/usr/local/bin")
        );
        for (Path directory : standardDirectories) {
            Path candidate = directory.resolve(executable);
            if (Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath();
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
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        Path defaultLibDir = Path.of(System.getProperty("user.home"), "tools/pgsql/usr/lib/x86_64-linux-gnu");
        if (Files.isDirectory(defaultLibDir)) {
            String currentLd = processBuilder.environment().getOrDefault("LD_LIBRARY_PATH", "");
            if (!currentLd.contains(defaultLibDir.toString())) {
                processBuilder.environment().put("LD_LIBRARY_PATH",
                        defaultLibDir + (currentLd.isEmpty() ? "" : java.io.File.pathSeparator + currentLd));
            }
        }
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command failed (" + exitCode + "): " + command[0] + "\n" + output);
        }
    }

    static void createDatabase(PostgresConfiguration configuration, String databaseName) throws SQLException {
        String adminUrl = withDatabaseName(configuration.jdbcUrl(), "postgres");
        try (Connection connection = DriverManager.getConnection(
                adminUrl, configuration.username(), configuration.password());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE \"" + databaseName + "\"");
        }
    }

    static void dropDatabase(PostgresConfiguration configuration, String databaseName) throws SQLException {
        String adminUrl = withDatabaseName(configuration.jdbcUrl(), "postgres");
        try (Connection connection = DriverManager.getConnection(
                adminUrl, configuration.username(), configuration.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE \"" + databaseName + "\"");
        }
    }

    static String withDatabaseName(String jdbcUrl, String databaseName) {
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

    record PostgresConfiguration(String jdbcUrl, String username, String password) {
    }

}
