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
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_LEAD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_LEAD_CONVERT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_OPPORTUNITY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_QUOTATION));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_QUOTATION_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_CONFIRM));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_INVOICE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_PAY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_TASK));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_REPORT));
    }

    @Test
    void financeCanInvoiceAndPayCrmOrders() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_INVOICE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_PAY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_CONFIRM));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_LEAD_CONVERT));
    }

    @Test
    void extensionWorkerHasLimitedCrmCommercialAccess() {
        Role r = Role.EXTENSION_WORKER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_UPDATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_LEAD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_TASK));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_LEAD_CONVERT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_QUOTATION_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_CONFIRM));
    }

    @Test
    void unauthorizedRolesCannotPayCrmOrders() {
        assertFalse(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.FARM_MANAGER, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.CONSULTANT, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.GENERAL_MANAGER, Permissions.CRM_ORDER_PAY));
    }

    @Test
    void generalManagerCanApproveQuotationsAndViewCrmReports() {
        Role r = Role.GENERAL_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_REPORT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CRM_QUOTATION_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CRM_ORDER_PAY));
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

    @Test
    void consultantHasOperationalButNotCommercialFinance() {
        Role r = Role.CONSULTANT;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_ACTIVITY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_DELIVERABLE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_ASSIGN));
    }

    @Test
    void financeCanInvoiceAndPayConsultancyButNotApproveDeliverables() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_INVOICE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_PAYMENT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_ASSIGN));
    }

    @Test
    void generalManagerHasConsultancyApproveAssignComplete() {
        Role r = Role.GENERAL_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_ASSIGN));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_COMPLETE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_CANCEL));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.CONSULTANCY_CONTRACT));
    }

    @Test
    void adminHasConsultancyReadOnly() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CONSULTANCY_READ));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CONSULTANCY_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CONSULTANCY_INVOICE));
    }

    @Test
    void operationsManagerHasConsultancyRead() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.OPERATIONS_MANAGER, Permissions.CONSULTANCY_READ));
    }

    @Test
    void hrOwnsEmployeeLeaveAndPayrollCapabilities() {
        Role r = Role.HR;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_UPDATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_TERMINATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_ATTENDANCE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_PROCESS));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_REPORT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_SELF));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.FINANCE_RECONCILE));
    }

    @Test
    void financeCanDisbursePayrollButNotHireOrTerminate() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_PROCESS));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_TERMINATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_UPDATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE_APPROVE));
    }

    @Test
    void consultantSeesOwnHrRecordOnly() {
        Role r = Role.CONSULTANT;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_SELF));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_READ));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_UPDATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_TERMINATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_ATTENDANCE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE_APPROVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL));
    }

    @Test
    void pharmacistHasNoHrRecordAccessBeyondTheirOwn() {
        Role r = Role.PHARMACIST;
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_READ));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_SELF));
    }

    @Test
    void generalManagerApprovesLeaveAndPayrollWithoutRunningIt() {
        Role r = Role.GENERAL_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_LEAVE_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.HR_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_PAYROLL_PROCESS));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.HR_TERMINATE));
    }

    @Test
    void noRoleCanBothApproveAndProcessPayrollExceptHr() {
        for (Role role : RolePermissionCatalog.canonicalRoles()) {
            boolean approves = RolePermissionCatalog.hasPermission(role, Permissions.HR_PAYROLL_APPROVE);
            boolean processes = RolePermissionCatalog.hasPermission(role, Permissions.HR_PAYROLL_PROCESS);
            assertTrue(!(approves && processes) || role == Role.HR,
                    "Payroll approval and disbursement must stay separated for " + role);
        }
    }

    // ─── Procurement RBAC ────────────────────────────────────────

    @Test
    void inventoryManagerCanReceiveAndApproveButNotPaySuppliers() {
        Role r = Role.INVENTORY_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_CREATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_UPDATE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_RECEIVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_CANCEL));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_INVOICE));
    }

    @Test
    void financeCanInvoiceAndPayButCannotReceiveGoods() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_INVOICE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_PAYMENT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_RECEIVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_APPROVE));
    }

    @Test
    void farmManagerCanCreateRequisitionButNotReceiveOrPay() {
        Role r = Role.FARM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_RECEIVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_INVOICE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_APPROVE));
    }

    @Test
    void generalManagerCanApproveProcurementWithoutWarehouseOrPayment() {
        Role r = Role.GENERAL_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_READ));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_APPROVE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_RECEIVE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.PROCUREMENT_CREATE));
    }

    @Test
    void adminHasProcurementReadOnly() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.PROCUREMENT_READ));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.PROCUREMENT_CREATE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.PROCUREMENT_RECEIVE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.PROCUREMENT_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.PROCUREMENT_INVOICE));
    }

    @Test
    void warehouseAndFinanceProcurementDutiesRemainSeparated() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.INVENTORY_MANAGER, Permissions.PROCUREMENT_RECEIVE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.INVENTORY_MANAGER, Permissions.PROCUREMENT_PAYMENT));
        assertTrue(RolePermissionCatalog.hasPermission(Role.FINANCE, Permissions.PROCUREMENT_PAYMENT));
        assertFalse(RolePermissionCatalog.hasPermission(Role.FINANCE, Permissions.PROCUREMENT_RECEIVE));
    }

    // ─── Executive RBAC ──────────────────────────────────────────

    @Test
    void generalManagerHasAllExecutivePermissionsIncludingFinance() {
        Role r = Role.GENERAL_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FINANCE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_SALES));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FARM));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_INVENTORY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_HR));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_REPORT));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.REPORTS_READ));
    }

    @Test
    void operationsManagerLacksExecutiveFinanceAndHr() {
        Role r = Role.OPERATIONS_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_SALES));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FARM));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_INVENTORY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FINANCE));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_HR));
    }

    @Test
    void crmManagerLacksExecutiveHr() {
        Role r = Role.CRM_MANAGER;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_SALES));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_HR));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FINANCE));
    }

    @Test
    void financeHasExecutiveFinanceButNotHrScope() {
        Role r = Role.FINANCE;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FINANCE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_REPORT));
        assertFalse(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_HR));
    }

    @Test
    void adminHasAllExecutivePermissions() {
        Role r = Role.ADMIN;
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FINANCE));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_SALES));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_FARM));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_INVENTORY));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_HR));
        assertTrue(RolePermissionCatalog.hasPermission(r, Permissions.EXECUTIVE_REPORT));
    }

    @Test
    void governanceReadGrantedToAdminGmAndFinance() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.GOVERNANCE_READ));
        assertTrue(RolePermissionCatalog.hasPermission(Role.GENERAL_MANAGER, Permissions.GOVERNANCE_READ));
        assertTrue(RolePermissionCatalog.hasPermission(Role.FINANCE, Permissions.GOVERNANCE_READ));
        assertFalse(RolePermissionCatalog.hasPermission(Role.FARM_WORKER, Permissions.GOVERNANCE_READ));
        assertFalse(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.GOVERNANCE_READ));
    }

    @Test
    void farmRolesDoNotReceiveExecutiveHr() {
        assertFalse(RolePermissionCatalog.hasPermission(Role.FARM_MANAGER, Permissions.EXECUTIVE_HR));
        assertFalse(RolePermissionCatalog.hasPermission(Role.FARM_WORKER, Permissions.EXECUTIVE_HR));
        assertFalse(RolePermissionCatalog.hasPermission(Role.FARM_MANAGER, Permissions.EXECUTIVE_DASHBOARD));
    }
}
