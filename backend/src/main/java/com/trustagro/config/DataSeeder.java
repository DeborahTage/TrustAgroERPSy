package com.trustagro.config;

import com.trustagro.masterdata.entity.MasterDataCategory;
import com.trustagro.masterdata.entity.MasterDataItem;
import com.trustagro.masterdata.repository.MasterDataItemRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "Trust@1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MasterDataItemRepository masterDataRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final List<Map.Entry<String, Role>> DEMO_USERS = List.of(
            Map.entry("admin@trustagro.com", Role.ADMIN),
            Map.entry("farmmanager@trustagro.com", Role.FARM_MANAGER),
            Map.entry("inventory@trustagro.com", Role.INVENTORY_MANAGER),
            Map.entry("pharmacy@trustagro.com", Role.PHARMACIST),
            Map.entry("vet@trustagro.com", Role.VETERINARIAN),
            Map.entry("finance@trustagro.com", Role.FINANCE),
            Map.entry("crm@trustagro.com", Role.CRM_MANAGER),
            Map.entry("marketing@trustagro.com", Role.MARKETING),
            Map.entry("training@trustagro.com", Role.TRAINING_OFFICER)
    );

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      MasterDataItemRepository masterDataRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterDataRepository = masterDataRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        refreshEnumCheckConstraints();
        migrateLegacyRoles();
        seedUsers();
        seedMasterData();
    }

    /**
     * Hibernate/H2 keeps stale CHECK constraints on enum columns after Role values change.
     * H2 may name them users_role_chk OR opaque names like CONSTRAINT_6A — drop all of them,
     * widen the column to VARCHAR, then optionally recreate a current-role CHECK.
     */
    private void refreshEnumCheckConstraints() {
        dropAllCheckConstraints("users");
        dropAllCheckConstraints("notifications");
        // Plain VARCHAR — no CHECK. Avoids H2 opaque constraints (CONSTRAINT_6A) blocking new Role values.
        alterToVarchar("users", "role", 64);
        alterToVarchar("notifications", "target_role", 64);
        dropAllCheckConstraints("users");
        dropAllCheckConstraints("notifications");
    }

    private void dropAllCheckConstraints(String tableName) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                            "WHERE LOWER(TABLE_NAME) = LOWER(?) AND CONSTRAINT_TYPE = 'CHECK'",
                    String.class,
                    tableName
            );
            // H2 also lists constraints here (older metadata)
            try {
                names.addAll(jdbcTemplate.queryForList(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS " +
                                "WHERE LOWER(TABLE_NAME) = LOWER(?) AND CONSTRAINT_TYPE = 'CHECK'",
                        String.class,
                        tableName
                ));
            } catch (Exception ignored) {
                // CONSTRAINTS view may not exist on all H2 versions
            }
            names.stream().distinct().forEach(name -> {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + name);
                    log.info("Dropped CHECK constraint {} on {}", name, tableName);
                } catch (Exception e) {
                    log.warn("Could not drop constraint {}: {}", name, e.getMessage());
                }
            });
            // Explicitly drop the known opaque H2 name from this DB
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS CONSTRAINT_6A");
            } catch (Exception ignored) {
                // ignore
            }
        } catch (Exception e) {
            log.warn("Could not list CHECK constraints on {}: {}", tableName, e.getMessage());
        }
    }

    private void alterToVarchar(String table, String column, int length) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " VARCHAR(" + length + ")");
            log.info("Altered {}.{} to VARCHAR({})", table, column, length);
        } catch (Exception e) {
            log.warn("Could not alter {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void recreateRoleCheck(String table, String column) {
        try {
            String allowed = Arrays.stream(Role.values())
                    .map(r -> "'" + r.name() + "'")
                    .collect(Collectors.joining(", "));
            String constraintName = table + "_" + column + "_chk";
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraintName);
            } catch (Exception ignored) {
                // ignore
            }
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName +
                            " CHECK (" + column + " IN (" + allowed + ") OR " + column + " IS NULL)"
            );
            log.info("Recreated CHECK constraint {} for {}.{}", constraintName, table, column);
        } catch (Exception e) {
            log.warn("Could not recreate CHECK on {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void migrateLegacyRoles() {
        Map<String, String> migrations = Map.of(
                "GENERAL_MANAGER", "ADMIN",
                "OPERATIONS_MANAGER", "ADMIN",
                "STORE_KEEPER", "INVENTORY_MANAGER",
                "PHARMACY_SALES", "PHARMACIST",
                "VETERINARY_OFFICER", "VETERINARIAN",
                "FINANCE_OFFICER", "FINANCE",
                "EXTENSION_WORKER", "CRM_MANAGER"
        );
        migrations.forEach((oldRole, newRole) -> {
            try {
                int updated = jdbcTemplate.update("UPDATE users SET role = ? WHERE role = ?", newRole, oldRole);
                if (updated > 0) log.info("Migrated {} users from {} to {}", updated, oldRole, newRole);
                int notifUpdated = jdbcTemplate.update(
                        "UPDATE notifications SET target_role = ? WHERE target_role = ?", newRole, oldRole);
                if (notifUpdated > 0) log.info("Migrated {} notifications from {} to {}", notifUpdated, oldRole, newRole);
            } catch (Exception e) {
                log.warn("Role migration {} → {} skipped: {}", oldRole, newRole, e.getMessage());
            }
        });
    }

    private void seedUsers() {
        for (Map.Entry<String, Role> entry : DEMO_USERS) {
            String email = entry.getKey();
            Role role = entry.getValue();
            if (userRepository.existsByEmail(email)) continue;
            User user = new User();
            user.setFullName(formatRoleName(role));
            user.setEmail(email);
            user.setPhone("+251900000000");
            user.setPassword(passwordEncoder.encode(Role.ADMIN.equals(role) ? "Admin@1234" : DEFAULT_PASSWORD));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            log.info("Demo user: {} / {} ({})", email, Role.ADMIN.equals(role) ? "Admin@1234" : DEFAULT_PASSWORD, role.name());
        }
    }

    private String formatRoleName(Role role) {
        return switch (role) {
            case ADMIN -> "System Administrator";
            case FARM_MANAGER -> "Farm Manager";
            case INVENTORY_MANAGER -> "Inventory Manager";
            case PHARMACIST -> "Pharmacist";
            case VETERINARIAN -> "Veterinarian";
            case FINANCE -> "Finance Officer";
            case CRM_MANAGER -> "CRM Manager";
            case MARKETING -> "Marketing Manager";
            case TRAINING_OFFICER -> "Training Officer";
            default -> role.name().replace('_', ' ');
        };
    }

    private void seedMasterData() {
        seedMDM(MasterDataCategory.BREED, "COBB_500", "Cobb 500", "Broiler breed", 1);
        seedMDM(MasterDataCategory.BREED, "ROSS_308", "Ross 308", "Broiler breed", 2);
        seedMDM(MasterDataCategory.BREED, "ISA_BROWN", "ISA Brown", "Pullet breed", 3);
        seedMDM(MasterDataCategory.BREED, "LOHMANN_BROWN", "Lohmann Brown", "Layer pullet", 4);

        seedMDM(MasterDataCategory.DRUG_TYPE, "ANTIBIOTIC", "Antibiotic", "Bacterial infections", 1);
        seedMDM(MasterDataCategory.DRUG_TYPE, "VACCINE", "Vaccine", "Preventative immunization", 2);
        seedMDM(MasterDataCategory.DRUG_TYPE, "VITAMIN", "Vitamin Supplement", "Nutritional supplement", 3);
        seedMDM(MasterDataCategory.DRUG_TYPE, "ANTHELMINTIC", "Dewormer", "Internal parasites", 4);

        seedMDM(MasterDataCategory.FEED_TYPE, "STARTER", "Starter Feed", "0-4 weeks", 1);
        seedMDM(MasterDataCategory.FEED_TYPE, "GROWER", "Grower Feed", "4-8 weeks", 2);
        seedMDM(MasterDataCategory.FEED_TYPE, "FINISHER", "Finisher Feed", "8+ weeks", 3);
        seedMDM(MasterDataCategory.FEED_TYPE, "LAYER_MASH", "Layer Mash", "Mature birds", 4);

        seedMDM(MasterDataCategory.DISEASE_TYPE, "VIRAL", "Viral", "Viral diseases", 1);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "BACTERIAL", "Bacterial", "Bacterial diseases", 2);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "FUNGAL", "Fungal", "Fungal diseases", 3);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "PARASITIC", "Parasitic", "Parasitic diseases", 4);

        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "BROILER", "Broiler Production", "Meat chickens", 1);
        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "PULLET", "Pullet Rearing", "Grow chicks to maturity", 2);
        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "LAYER", "Layer Rearing", "Mature laying birds", 3);
        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "BREEDING", "Breeding Stock", "Parent stock", 4);
    }

    private void seedMDM(MasterDataCategory category, String value, String label, String description, int sortOrder) {
        if (!masterDataRepository.existsByCategoryAndValue(category, value)) {
            MasterDataItem item = new MasterDataItem();
            item.setCategory(category);
            item.setValue(value);
            item.setLabel(label);
            item.setDescription(description);
            item.setSortOrder(sortOrder);
            item.setActive(true);
            masterDataRepository.save(item);
        }
    }
}
