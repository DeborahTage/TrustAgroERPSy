package com.trustagro.config;

import com.trustagro.masterdata.entity.MasterDataCategory;
import com.trustagro.masterdata.entity.MasterDataItem;
import com.trustagro.masterdata.repository.MasterDataItemRepository;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.ItemCategory;
import com.trustagro.inventory.entity.ItemStatus;
import com.trustagro.inventory.entity.ItemUnit;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final InventoryItemRepository inventoryItemRepository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean seedDemoUsers;
    private final boolean seedDemoMasterData;

    private static final List<Map.Entry<String, Role>> DEMO_USERS = List.of(
            Map.entry("admin@trustagro.com", Role.ADMIN),
            Map.entry("gm@trustagro.com", Role.GENERAL_MANAGER),
            Map.entry("farmmanager@trustagro.com", Role.FARM_MANAGER),
            Map.entry("inventory@trustagro.com", Role.INVENTORY_MANAGER),
            Map.entry("pharmacy@trustagro.com", Role.PHARMACIST),
            Map.entry("vet@trustagro.com", Role.VETERINARIAN),
            Map.entry("finance@trustagro.com", Role.FINANCE),
            Map.entry("crm@trustagro.com", Role.CRM_MANAGER),
            Map.entry("marketing@trustagro.com", Role.MARKETING_MANAGER),
            Map.entry("training@trustagro.com", Role.TRAINING_COORDINATOR)
    );

    @Autowired
    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      MasterDataItemRepository masterDataRepository, InventoryItemRepository inventoryItemRepository,
                      JdbcTemplate jdbcTemplate,
                      @Value("${app.seed.demo-users:false}") boolean seedDemoUsers,
                      @Value("${app.seed.demo-master-data:false}") boolean seedDemoMasterData) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.masterDataRepository = masterDataRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.seedDemoUsers = seedDemoUsers;
        this.seedDemoMasterData = seedDemoMasterData;
    }

        public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      MasterDataItemRepository masterDataRepository, JdbcTemplate jdbcTemplate,
                      boolean seedDemoUsers, boolean seedDemoMasterData) {
        this(userRepository, passwordEncoder, masterDataRepository, null, jdbcTemplate, seedDemoUsers, seedDemoMasterData);
    }

    @Override
    public void run(String... args) {
        refreshEnumCheckConstraints();
        migrateLegacyRoles();
        if (seedDemoUsers) {
            seedUsers();
        } else {
            log.info("Demo user seeding disabled (app.seed.demo-users=false)");
        }
        if (seedDemoMasterData) {
            seedMasterData();
            if (inventoryItemRepository != null) {
                seedVeterinaryPharmacyItems();
            }
        } else {
            log.info("Demo master-data seeding disabled (app.seed.demo-master-data=false)");
        }
    }

    /**
     * Hibernate/H2 keeps stale CHECK constraints on enum columns after Role values change.
     * H2-specific; safely no-ops / warns on PostgreSQL and other dialects.
     */
    private void refreshEnumCheckConstraints() {
        try {
            dropAllCheckConstraints("users");
            dropAllCheckConstraints("notifications");
            alterToVarchar("users", "role", 64);
            alterToVarchar("notifications", "target_role", 64);
            dropAllCheckConstraints("users");
            dropAllCheckConstraints("notifications");
        } catch (Exception e) {
            log.warn("Enum CHECK constraint refresh skipped (non-H2 or unsupported dialect): {}", e.getMessage());
        }
    }

    private void dropAllCheckConstraints(String tableName) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                            "WHERE LOWER(TABLE_NAME) = LOWER(?) AND CONSTRAINT_TYPE = 'CHECK'",
                    String.class,
                    tableName
            );
            try {
                names.addAll(jdbcTemplate.queryForList(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS " +
                                "WHERE LOWER(TABLE_NAME) = LOWER(?) AND CONSTRAINT_TYPE = 'CHECK'",
                        String.class,
                        tableName
                ));
            } catch (Exception ignored) {
                // CONSTRAINTS view may not exist on all H2 versions / Postgres
            }
            names.stream().distinct().forEach(name -> {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + name);
                    log.info("Dropped CHECK constraint {} on {}", name, tableName);
                } catch (Exception e) {
                    log.warn("Could not drop constraint {}: {}", name, e.getMessage());
                }
            });
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

    /**
     * Migrates deprecated role strings onto canonical roles.
     * GENERAL_MANAGER, OPERATIONS_MANAGER, EXTENSION_WORKER are canonical — do not remap them.
     * Note: earlier builds may have remapped GM/OPS → ADMIN; those rows cannot be auto-recovered.
     */
    private void migrateLegacyRoles() {
        Map<String, String> migrations = Map.of(
                "STORE_KEEPER", "INVENTORY_MANAGER",
                "PHARMACY_SALES", "PHARMACIST",
                "VETERINARY_OFFICER", "VETERINARIAN",
                "FINANCE_OFFICER", "FINANCE",
                "MARKETING", "MARKETING_MANAGER",
                "TRAINING_OFFICER", "TRAINING_COORDINATOR",
                "CRM_AGENT", "CRM_MANAGER"
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
            log.info("Demo user created: {} ({})", email, role.name());
        }
    }

    private String formatRoleName(Role role) {
        return switch (role) {
            case ADMIN -> "System Administrator";
            case GENERAL_MANAGER -> "General Manager (Trust Agro Consulting & Farming)";
            case OPERATIONS_MANAGER -> "Operations Manager";
            case SECRETARY -> "Secretary";
            case FARM_MANAGER -> "Farm Manager";
            case FARM_WORKER -> "Farm Worker";
            case INVENTORY_MANAGER -> "Inventory Manager";
            case PHARMACIST -> "Pharmacist";
            case VETERINARIAN -> "Veterinarian";
            case FINANCE -> "Finance";
            case HR -> "HR";
            case CRM_MANAGER -> "CRM Manager";
            case MARKETING_MANAGER, MARKETING -> "Marketing Manager";
            case TRAINING_COORDINATOR, TRAINING_OFFICER -> "Training Coordinator";
            case CONSULTANT -> "Consultant";
            case EXTENSION_WORKER -> "Extension Worker";
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

        seedMDM(MasterDataCategory.DISEASE_TYPE, "VIRAL", "Viral", "Viral diseases", 1);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "BACTERIAL", "Bacterial", "Bacterial diseases", 2);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "FUNGAL", "Fungal", "Fungal diseases", 3);
        seedMDM(MasterDataCategory.DISEASE_TYPE, "PARASITIC", "Parasitic", "Parasitic diseases", 4);

        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "BROILER", "Broiler Production", "Meat chickens", 1);
        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "PULLET", "Pullet Rearing", "Grow chicks to maturity", 2);
        seedMDM(MasterDataCategory.FLOCK_PURPOSE, "BREEDING", "Breeding Stock", "Parent stock", 3);
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

    private void seedVeterinaryPharmacyItems() {
        // DRUG
        seedPharmacyItem("PHARM-ASHIENERO-10-ORAL", "ASHIENERO 10 % ORAL", ItemCategory.DRUG, ItemUnit.PIECE, "Antibiotic", "600.00");
        seedPharmacyItem("PHARM-TOLTRACOX", "TOLTRACOX", ItemCategory.DRUG, ItemUnit.PIECE, "Anticoccidial", "800.00");
        seedPharmacyItem("PHARM-LIMOXIN", "LIMOXIN", ItemCategory.DRUG, ItemUnit.PIECE, "Antibiotic", "1200.00");
        seedPharmacyItem("PHARM-RESPIMINT-ORAL", "RESPIMINT ORAL", ItemCategory.DRUG, ItemUnit.PIECE, "Respiratory treatment", "2000.00");
        seedPharmacyItem("PHARM-COXASAN-ORAL", "COXASAN ORAL", ItemCategory.DRUG, ItemUnit.PIECE, "Anticoccidial", "1800.00");
        seedPharmacyItem("PHARM-AMPROLIN-300-WS", "AMPROLIN-300 ws", ItemCategory.DRUG, ItemUnit.PIECE, "Anticoccidial", "650.00");
        seedPharmacyItem("PHARM-PIPERIN-WS", "PIPERIN ws", ItemCategory.DRUG, ItemUnit.PIECE, "Antiparasitic", "600.00");

        // VACCINE
        seedPharmacyItem("PHARM-LASOTA-VACCINE", "LASOTA VACCINE", ItemCategory.VACCINE, ItemUnit.DOSE, "Poultry vaccine", "1.50");
        seedPharmacyItem("PHARM-TYPHOID-VACCINE", "TYPHOID VACCINE", ItemCategory.VACCINE, ItemUnit.DOSE, "Poultry vaccine", "3.00");
        seedPharmacyItem("PHARM-IB-VACCINE", "IB VACCINE", ItemCategory.VACCINE, ItemUnit.DOSE, "Poultry vaccine", "2.20");

        // VITAMIN (Supplements & Feed Additives)
        seedPharmacyItem("PHARM-VITABOOST", "VITABOOST", ItemCategory.VITAMIN, ItemUnit.PIECE, "Vitamin supplement", "450.00");
        seedPharmacyItem("PHARM-TEKNOACID", "TEKNOACID", ItemCategory.VITAMIN, ItemUnit.PIECE, "Feed additive", "1500.00");
        seedPharmacyItem("PHARM-AMINOVIT", "AMINOVIT", ItemCategory.VITAMIN, ItemUnit.PIECE, "Amino acid supplement", "525.00");
        seedPharmacyItem("PHARM-VIT-ADEK", "VIT-ADEK", ItemCategory.VITAMIN, ItemUnit.PIECE, "Vitamin supplement", "750.00");
        seedPharmacyItem("PHARM-SPECIAL-LIQUID", "SPECIAL LIQUID", ItemCategory.VITAMIN, ItemUnit.PIECE, "Supplement", "700.00");
        seedPharmacyItem("PHARM-MAXIVIT", "MAXIVIT", ItemCategory.VITAMIN, ItemUnit.PIECE, "Vitamin supplement", "400.00");
        seedPharmacyItem("PHARM-EGG-STIMULANT", "EGG STIMULANT", ItemCategory.VITAMIN, ItemUnit.PIECE, "Layer supplement", "500.00");
        seedPharmacyItem("PHARM-SYNBIOTIC", "SYNBIOTIC", ItemCategory.VITAMIN, ItemUnit.PIECE, "Gut health supplement", "500.00");
        seedPharmacyItem("PHARM-ARMOR", "ARMOR", ItemCategory.VITAMIN, ItemUnit.PIECE, "Supplement", "700.00");
        seedPharmacyItem("PHARM-INTRASAN-PH-ORAL", "INTRASAN PH ORAL", ItemCategory.VITAMIN, ItemUnit.PIECE, "Water acidifier & gut health", "1500.00");
        seedPharmacyItem("PHARM-GARVITAL-ORAL", "GARVITAL ORAL", ItemCategory.VITAMIN, ItemUnit.PIECE, "Supplement", "1800.00");
        seedPharmacyItem("PHARM-CALCIMIN-P-ORAL", "CALCIMIN-P ORAL", ItemCategory.VITAMIN, ItemUnit.PIECE, "Mineral supplement", "1800.00");
        seedPharmacyItem("PHARM-INTROVIT-A-ORAL", "INTROVIT A+ ORAL", ItemCategory.VITAMIN, ItemUnit.PIECE, "Vitamin supplement", "500.00");
        seedPharmacyItem("PHARM-INTROVIT-A-WS", "INTROVIT A+ WS", ItemCategory.VITAMIN, ItemUnit.PIECE, "Water soluble vitamin", "500.00");

        // FEED
        seedPharmacyItem("PHARM-LAYER-FEED", "LAYER FEED", ItemCategory.FEED, ItemUnit.PIECE, "Layer feed", "9050.00");
        seedPharmacyItem("PHARM-PULLET-FEED", "PULLET FEED", ItemCategory.FEED, ItemUnit.PIECE, "Pullet feed", "8600.00");
        seedPharmacyItem("PHARM-MOLASES", "MOLASES", ItemCategory.FEED, ItemUnit.KG, "Feed ingredient", "300.00");
        seedPharmacyItem("PHARM-FINISHER", "FINISHER", ItemCategory.FEED, ItemUnit.PIECE, "Finisher feed", "650.00");

        // EQUIPMENT
        seedPharmacyItem("PHARM-MANUAL-DRINKER", "MANUAL DRINKER", ItemCategory.EQUIPMENT, ItemUnit.PIECE, "Poultry equipment", "370.00");
        seedPharmacyItem("PHARM-FEEDER", "FEEDER", ItemCategory.EQUIPMENT, ItemUnit.PIECE, "Poultry equipment", "500.00");
        seedPharmacyItem("PHARM-AUTOMATIC-DRINKER", "AUTOMATIC DRINKER", ItemCategory.EQUIPMENT, ItemUnit.PIECE, "Poultry equipment", "3050.00");

        // FARM MATERIAL
        seedPharmacyItem("PHARM-HYDROGEN-PEROXIDE", "HYDROGEN PER OXIDE", ItemCategory.FARM_MATERIAL, ItemUnit.LITER, "Disinfectant and biosecurity", "450.00");
        // Reset mock stock quantities so unverified piece counts are not populated
        jdbcTemplate.update("UPDATE inventory_items SET current_stock = NULL WHERE sku LIKE 'PHARM-%'");
    }

    private void seedPharmacyItem(String sku, String name, ItemCategory category, ItemUnit unit, String subcategory, String retailPrice) {
        InventoryItem item = inventoryItemRepository.findBySku(sku).orElseGet(InventoryItem::new);
        item.setSku(sku);
        item.setItemName(name);
        item.setCategory(category);
        item.setSubcategory(subcategory);
        item.setUnit(unit);
        item.setCurrentStock(null);
        item.setReorderPoint(0.0);
        item.setReorderQty(0.0);
        item.setRetailPrice(new BigDecimal(retailPrice));
        item.setSellingPrice(new BigDecimal(retailPrice));
        item.setStatus(ItemStatus.ACTIVE);
        item.setStorageLocation("Pharmacy POS");
        inventoryItemRepository.save(item);
    }
}
