package com.trustagro.security;

import com.trustagro.user.entity.Role;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused authorization boundary tests against the authoritative catalog
 * and a source guard for invalid CRM_AGENT references.
 */
class RolePermissionCatalogTest {

    @Test
    void pharmacistCannotStockInOrTransfer() {
        Role r = Role.PHARMACIST;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PHARMACY_SALE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PHARMACY_DISPENSE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_READ));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_STOCK_IN));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_TRANSFER));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_PO));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_STOCK_OUT));
    }

    @Test
    void pharmacistCanPharmacySaleAndDispense() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.PHARMACY_SALE));
        assertTrue(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.PHARMACY_DISPENSE));
        assertTrue(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.PHARMACY_READ));
    }

    @Test
    void veterinarianCanPrescribeButNotFinanceOrInventoryWrite() {
        Role r = Role.VETERINARIAN;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.VETERINARY_PRESCRIBE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.VETERINARY_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_STOCK_IN));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PHARMACY_SALE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.USERS_CREATE));
    }

    @Test
    void financeCanAccessFinanceButNotVeterinaryActions() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.VETERINARY_PRESCRIBE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.VETERINARY_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PHARMACY_DISPENSE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_STOCK_IN));
    }

    @Test
    void crmManagerCanAccessCrmOrdersCapabilities() {
        Role r = Role.CRM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_UPDATE));
    }

    @Test
    void inventoryManagerCanPerformInventoryOperations() {
        Role r = Role.INVENTORY_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_STOCK_IN));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_TRANSFER));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.INVENTORY_PO));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
    }

    @Test
    void farmManagerCannotInvoiceOrPayChickenSales() {
        Role r = Role.FARM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CONFIRM));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CANCEL));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
    }

    @Test
    void financeCanInvoiceAndPayButNotConfirmFlockDecrement() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_INVOICE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CONFIRM));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CANCEL));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FARM_UPDATE));
    }

    @Test
    void crmCannotManipulateFinanceLedgerOrFlockPopulation() {
        Role r = Role.CRM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_READ));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_CONFIRM));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CHICKEN_SALE_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
    }

    @Test
    void adminHasChickenSaleReadOnly() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CHICKEN_SALE_READ));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CHICKEN_SALE_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CHICKEN_SALE_CONFIRM));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CHICKEN_SALE_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CHICKEN_SALE_PAY));
    }

    @Test
    void farmManagerCannotAccessFinanceAdministration() {
        Role r = Role.FARM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.FARM_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_APPROVE));
    }

    @Test
    void adminHasNoWildcardAndNoDepartmentalWriteShortcuts() {
        List<String> admin = RolePermissionCatalog.permissionsFor(Role.ADMIN);
        assertFalse(admin.contains("*"));
        assertTrue(admin.contains(Permissions.USERS_CREATE));
        assertTrue(admin.contains(Permissions.AUDIT_READ));
        assertTrue(admin.contains(Permissions.FARM_READ));
        assertFalse(admin.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(admin.contains(Permissions.PHARMACY_SALE));
        assertFalse(admin.contains(Permissions.VETERINARY_PRESCRIBE));
        assertFalse(admin.contains(Permissions.FINANCE_CREATE));
    }

    @Test
    void legacyRolesNormalizeToCanonicalPermissions() {
        assertEquals(Role.INVENTORY_MANAGER, RoleCompatibility.normalize(Role.STORE_KEEPER));
        assertEquals(Role.PHARMACIST, RoleCompatibility.normalize(Role.PHARMACY_SALES));
        assertEquals(Role.MARKETING_MANAGER, RoleCompatibility.normalize(Role.MARKETING));
        assertEquals(Role.TRAINING_COORDINATOR, RoleCompatibility.normalize(Role.TRAINING_OFFICER));
        assertTrue(RolePermissionCatalog.hasPermission(Role.STORE_KEEPER, Permissions.INVENTORY_STOCK_IN));
        assertFalse(RolePermissionCatalog.hasPermission(Role.PHARMACY_SALES, Permissions.INVENTORY_STOCK_IN));
    }

    @Test
    void crmAgentAuthorizationNoLongerExistsInControllers() throws Exception {
        Path controllers = Path.of("src/main/java/com/trustagro");
        try (Stream<Path> paths = Files.walk(controllers)) {
            List<Path> hits = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("CRM_AGENT");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList();
            // DataSeeder may mention CRM_AGENT as a migration source — that is OK
            assertTrue(hits.stream().allMatch(p -> p.getFileName().toString().equals("DataSeeder.java")),
                    "CRM_AGENT must not appear in authorization annotations: " + hits);
        }
    }

    @Test
    void canonicalRoleSetIsComplete() {
        List<Role> canonical = RolePermissionCatalog.canonicalRoles();
        assertTrue(canonical.contains(Role.GENERAL_MANAGER));
        assertTrue(canonical.contains(Role.SECRETARY));
        assertTrue(canonical.contains(Role.HR));
        assertTrue(canonical.contains(Role.FARM_WORKER));
        assertTrue(canonical.contains(Role.MARKETING_MANAGER));
        assertTrue(canonical.contains(Role.TRAINING_COORDINATOR));
        assertTrue(canonical.contains(Role.CONSULTANT));
        assertFalse(canonical.contains(Role.STORE_KEEPER));
        assertTrue(canonical.contains(Role.CRM_MANAGER));
    }

    @Test
    void trainingCoordinatorHasWorkflowPermissionsButNotFinanceCreate() {
        Role r = Role.TRAINING_COORDINATOR;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_REGISTER));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_CERTIFICATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
    }

    @Test
    void financeCanRecordTrainingPaymentsButNotPublishPrograms() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_PAYMENT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_READ));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_PUBLISH));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.TRAINING_CERTIFICATE));
    }
}
