package com.trustagro.qa.uat;

import com.trustagro.security.Permissions;
import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 15 UAT — role capability matrix against the canonical permission catalog.
 * ACTUALLY EXECUTED (unit). Does not simulate browser login.
 */
class RoleUatMatrixTest {

    @Test
    void allSixteenCanonicalRolesPresentIncludingSecretary() {
        List<Role> roles = RolePermissionCatalog.canonicalRoles();
        assertEquals(16, roles.size());
        assertTrue(roles.contains(Role.SECRETARY));
        assertTrue(roles.contains(Role.EXTENSION_WORKER));
        assertTrue(roles.contains(Role.ADMIN));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {
            "ADMIN", "GENERAL_MANAGER", "OPERATIONS_MANAGER", "SECRETARY", "FINANCE", "HR",
            "FARM_MANAGER", "FARM_WORKER", "VETERINARIAN", "INVENTORY_MANAGER", "PHARMACIST",
            "CRM_MANAGER", "MARKETING_MANAGER", "TRAINING_COORDINATOR", "CONSULTANT", "EXTENSION_WORKER"
    })
    void everyCanonicalRoleHasHrSelfAndNonEmptyMatrix(Role role) {
        List<String> perms = RolePermissionCatalog.permissionsFor(role);
        assertFalse(perms.isEmpty(), role + " has empty permissions");
        assertTrue(perms.contains(Permissions.HR_SELF), role + " missing HR_SELF");
    }

    @Test
    void adminIsGovernanceNotOperationalWriter() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.ADMIN);
        assertTrue(p.contains(Permissions.USERS_CREATE));
        assertTrue(p.contains(Permissions.AUDIT_READ));
        assertTrue(p.contains(Permissions.EXECUTIVE_DASHBOARD));
        assertFalse(p.contains(Permissions.FINANCE_CREATE));
        assertFalse(p.contains(Permissions.PHARMACY_SALE));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(p.contains(Permissions.CRM_ORDER_PAY));
        assertFalse(p.contains(Permissions.HR_PAYROLL_PROCESS));
    }

    @Test
    void generalManagerApprovesButDoesNotOwnOperationalWrites() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.GENERAL_MANAGER);
        assertTrue(p.contains(Permissions.EXECUTIVE_DASHBOARD));
        assertTrue(p.contains(Permissions.PROCUREMENT_APPROVE));
        assertTrue(p.contains(Permissions.FINANCE_APPROVE));
        assertFalse(p.contains(Permissions.FINANCE_CREATE));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(p.contains(Permissions.PHARMACY_DISPENSE));
        assertFalse(p.contains(Permissions.USERS_CREATE));
    }

    @Test
    void farmWorkerCannotTouchFinanceHrUsersOrProcurementApprove() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.FARM_WORKER);
        assertTrue(p.contains(Permissions.FARM_READ));
        assertTrue(p.contains(Permissions.FARM_UPDATE));
        assertFalse(p.contains(Permissions.FINANCE_READ));
        assertFalse(p.contains(Permissions.HR_PAYROLL));
        assertFalse(p.contains(Permissions.USERS_UPDATE));
        assertFalse(p.contains(Permissions.PROCUREMENT_APPROVE));
        assertFalse(p.contains(Permissions.PROCUREMENT_PAYMENT));
        assertFalse(p.contains(Permissions.ROLES_MANAGE));
    }

    @Test
    void pharmacistIsNotInventoryManager() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.PHARMACIST);
        assertTrue(p.contains(Permissions.PHARMACY_SALE));
        assertTrue(p.contains(Permissions.PHARMACY_DISPENSE));
        assertTrue(p.contains(Permissions.INVENTORY_READ));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_OUT));
        assertFalse(p.contains(Permissions.INVENTORY_TRANSFER));
        assertFalse(p.contains(Permissions.PROCUREMENT_RECEIVE));
    }

    @Test
    void veterinarianCannotDispenseOrSellPharmacy() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.VETERINARIAN);
        assertTrue(p.contains(Permissions.VETERINARY_PRESCRIBE));
        assertFalse(p.contains(Permissions.PHARMACY_DISPENSE));
        assertFalse(p.contains(Permissions.PHARMACY_SALE));
    }

    @Test
    void crmManagerCannotPayrollOrPharmacyAdminOrInventoryWrite() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.CRM_MANAGER);
        assertTrue(p.contains(Permissions.CRM_LEAD));
        assertTrue(p.contains(Permissions.CRM_ORDER));
        assertFalse(p.contains(Permissions.HR_PAYROLL));
        assertFalse(p.contains(Permissions.PHARMACY_SALE));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(p.contains(Permissions.FINANCE_RECONCILE));
        assertFalse(p.contains(Permissions.USERS_CREATE));
    }

    @Test
    void extensionWorkerCannotRunCommercialQuoteOrderPay() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.EXTENSION_WORKER);
        assertTrue(p.contains(Permissions.CRM_LEAD));
        assertTrue(p.contains(Permissions.CRM_TASK));
        assertFalse(p.contains(Permissions.CRM_OPPORTUNITY));
        assertFalse(p.contains(Permissions.CRM_QUOTATION));
        assertFalse(p.contains(Permissions.CRM_ORDER));
        assertFalse(p.contains(Permissions.CRM_ORDER_PAY));
        assertFalse(p.contains(Permissions.FINANCE_CREATE));
    }

    @Test
    void financeCanSettleRevenueButNotPrescribeOrManageUsers() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.FINANCE);
        assertTrue(p.contains(Permissions.FINANCE_CREATE));
        assertTrue(p.contains(Permissions.CRM_ORDER_PAY));
        assertTrue(p.contains(Permissions.PROCUREMENT_PAYMENT));
        assertFalse(p.contains(Permissions.VETERINARY_PRESCRIBE));
        assertFalse(p.contains(Permissions.USERS_CREATE));
        assertFalse(p.contains(Permissions.FARM_CREATE));
    }

    @Test
    void trainingCoordinatorHasNoFinanceAdmin() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.TRAINING_COORDINATOR);
        assertTrue(p.contains(Permissions.TRAINING_REGISTER));
        assertFalse(p.contains(Permissions.FINANCE_CREATE));
        assertFalse(p.contains(Permissions.TRAINING_PAYMENT)); // payment is finance
    }

    @Test
    void consultantCannotInvoiceOrTouchInventoryHr() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.CONSULTANT);
        assertTrue(p.contains(Permissions.CONSULTANCY_ACTIVITY));
        assertFalse(p.contains(Permissions.CONSULTANCY_INVOICE));
        assertFalse(p.contains(Permissions.CONSULTANCY_PAYMENT));
        assertFalse(p.contains(Permissions.INVENTORY_STOCK_IN));
        assertFalse(p.contains(Permissions.HR_PAYROLL));
    }

    @Test
    void secretaryIsReadOnlyAssistRole() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.SECRETARY);
        assertTrue(p.contains(Permissions.CRM_READ));
        assertTrue(p.contains(Permissions.FARM_READ));
        assertTrue(p.contains(Permissions.TRAINING_READ));
        assertFalse(p.contains(Permissions.CRM_LEAD));
        assertFalse(p.contains(Permissions.FINANCE_READ));
        assertFalse(p.contains(Permissions.HR_PAYROLL));
        assertFalse(p.contains(Permissions.USERS_UPDATE));
    }

    @Test
    void marketingCannotMutateFinanceHrInventoryOps() {
        List<String> p = RolePermissionCatalog.permissionsFor(Role.MARKETING_MANAGER);
        assertTrue(p.contains(Permissions.MARKETING_CREATE));
        assertFalse(p.contains(Permissions.FINANCE_CREATE));
        assertFalse(p.contains(Permissions.HR_UPDATE));
        assertFalse(p.contains(Permissions.INVENTORY_CREATE));
        assertFalse(p.contains(Permissions.FARM_UPDATE));
    }

    @Test
    void forbiddenMatrix_crossRolePrivilegeEscalationDenied() {
        Set<String> farmWorker = Set.copyOf(RolePermissionCatalog.permissionsFor(Role.FARM_WORKER));
        Set<String> pharmacist = Set.copyOf(RolePermissionCatalog.permissionsFor(Role.PHARMACIST));
        Set<String> consultant = Set.copyOf(RolePermissionCatalog.permissionsFor(Role.CONSULTANT));

        assertFalse(farmWorker.contains(Permissions.PROCUREMENT_PAYMENT));
        assertFalse(pharmacist.contains(Permissions.INVENTORY_PO));
        assertFalse(consultant.contains(Permissions.FINANCE_RECONCILE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.CRM_MANAGER, Permissions.ROLES_MANAGE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.EXTENSION_WORKER, Permissions.CRM_ORDER_CONFIRM));
    }
}
