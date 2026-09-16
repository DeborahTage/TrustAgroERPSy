package com.trustagro.executive;

import com.trustagro.security.Permissions;
import com.trustagro.user.entity.Role;
import com.trustagro.security.RolePermissionCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Dedicated RBAC boundary checks for the executive layer. */
class ExecutiveRbacTest {

    @Test
    void gmHasExecutiveFinance() {
        assertTrue(RolePermissionCatalog.hasPermission(Role.GENERAL_MANAGER, Permissions.EXECUTIVE_FINANCE));
        assertTrue(RolePermissionCatalog.hasPermission(Role.GENERAL_MANAGER, Permissions.EXECUTIVE_HR));
    }

    @Test
    void opsLacksFinanceAndHr() {
        assertFalse(RolePermissionCatalog.hasPermission(Role.OPERATIONS_MANAGER, Permissions.EXECUTIVE_FINANCE));
        assertFalse(RolePermissionCatalog.hasPermission(Role.OPERATIONS_MANAGER, Permissions.EXECUTIVE_HR));
        assertTrue(RolePermissionCatalog.hasPermission(Role.OPERATIONS_MANAGER, Permissions.EXECUTIVE_DASHBOARD));
    }

    @Test
    void crmLacksExecutiveHr() {
        assertFalse(RolePermissionCatalog.hasPermission(Role.CRM_MANAGER, Permissions.EXECUTIVE_HR));
        assertTrue(RolePermissionCatalog.hasPermission(Role.CRM_MANAGER, Permissions.EXECUTIVE_SALES));
    }
}
