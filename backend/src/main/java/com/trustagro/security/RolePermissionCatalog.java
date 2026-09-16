package com.trustagro.security;

import com.trustagro.user.entity.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.trustagro.security.Permissions.*;

/**
 * Single authoritative role → permission matrix.
 * RoleSeeder, AuthService, and CustomUserDetailsService all consume this catalog.
 * No wildcard {@code *} — ADMIN gets explicit admin + cross-module read.
 */
public final class RolePermissionCatalog {

    private static final Map<Role, Set<String>> MATRIX = new EnumMap<>(Role.class);

    static {
        put(Role.ADMIN,
                USERS_READ, USERS_CREATE, USERS_UPDATE, USERS_DELETE, ROLES_MANAGE,
                SETTINGS_READ, SETTINGS_UPDATE, AUDIT_READ, GOVERNANCE_READ, REPORTS_READ,
                MASTERDATA_READ, MASTERDATA_UPDATE, NOTIFICATIONS_READ,
                FARM_READ, VETERINARY_READ, INVENTORY_READ, PHARMACY_READ,
                FINANCE_READ, CRM_READ, MARKETING_READ, TRAINING_READ,
                CONSULTANCY_READ, HR_READ, CHICKEN_SALE_READ, PROCUREMENT_READ,
                EXECUTIVE_DASHBOARD, EXECUTIVE_FINANCE, EXECUTIVE_SALES, EXECUTIVE_FARM,
                EXECUTIVE_INVENTORY, EXECUTIVE_HR, EXECUTIVE_REPORT
        );

        put(Role.GENERAL_MANAGER,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ, AUDIT_READ, GOVERNANCE_READ,
                FARM_READ, VETERINARY_READ, INVENTORY_READ, PHARMACY_READ,
                FINANCE_READ, FINANCE_APPROVE, CRM_READ, CRM_REPORT, CRM_QUOTATION_APPROVE, MARKETING_READ,
                TRAINING_READ, CONSULTANCY_READ, CONSULTANCY_APPROVE, CONSULTANCY_ASSIGN,
                CONSULTANCY_COMPLETE, CONSULTANCY_REPORT, CONSULTANCY_CANCEL, CONSULTANCY_CONTRACT,
                HR_READ, HR_LEAVE_APPROVE, HR_PAYROLL_APPROVE, HR_REPORT, CHICKEN_SALE_READ,
                PROCUREMENT_READ, PROCUREMENT_APPROVE, PROCUREMENT_REPORT,
                EXECUTIVE_DASHBOARD, EXECUTIVE_FINANCE, EXECUTIVE_SALES, EXECUTIVE_FARM,
                EXECUTIVE_INVENTORY, EXECUTIVE_HR, EXECUTIVE_REPORT
        );

        put(Role.OPERATIONS_MANAGER,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ,
                FARM_READ, FARM_UPDATE, VETERINARY_READ, INVENTORY_READ,
                PHARMACY_READ, CRM_READ, CHICKEN_SALE_READ, CHICKEN_SALE_CONFIRM,
                CONSULTANCY_READ, PROCUREMENT_READ, PROCUREMENT_APPROVE, PROCUREMENT_REPORT,
                EXECUTIVE_DASHBOARD, EXECUTIVE_SALES, EXECUTIVE_FARM, EXECUTIVE_INVENTORY, EXECUTIVE_REPORT
        );

        put(Role.SECRETARY,
                NOTIFICATIONS_READ, SETTINGS_READ, CRM_READ, REPORTS_READ,
                FARM_READ, TRAINING_READ
        );

        put(Role.FINANCE,
                FINANCE_READ, FINANCE_CREATE, FINANCE_APPROVE, FINANCE_RECONCILE,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ, GOVERNANCE_READ,
                CHICKEN_SALE_READ, CHICKEN_SALE_INVOICE, CHICKEN_SALE_PAY,
                TRAINING_READ, TRAINING_PAYMENT, TRAINING_REPORT,
                CONSULTANCY_READ, CONSULTANCY_PAYMENT, CONSULTANCY_INVOICE, CONSULTANCY_REPORT,
                CRM_READ, CRM_ORDER_INVOICE, CRM_ORDER_PAY, CRM_REPORT,
                // Payroll cost visibility and disbursement — no employee record or attendance writes
                HR_READ, HR_PAYROLL, HR_PAYROLL_PROCESS, HR_REPORT,
                // AP invoice approve & supplier payment — no warehouse receive
                PROCUREMENT_READ, PROCUREMENT_INVOICE, PROCUREMENT_PAYMENT, PROCUREMENT_REPORT,
                EXECUTIVE_DASHBOARD, EXECUTIVE_FINANCE, EXECUTIVE_REPORT
        );

        put(Role.HR,
                HR_READ, HR_CREATE, HR_UPDATE, HR_TERMINATE, HR_ATTENDANCE,
                HR_LEAVE, HR_LEAVE_APPROVE, HR_PAYROLL, HR_PAYROLL_APPROVE,
                HR_PAYROLL_PROCESS, HR_REPORT, HR_SELF, REPORTS_READ,
                NOTIFICATIONS_READ, SETTINGS_READ
        );

        put(Role.FARM_MANAGER,
                FARM_READ, FARM_CREATE, FARM_UPDATE, FARM_DELETE,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ, INVENTORY_READ,
                CHICKEN_SALE_READ, CHICKEN_SALE_CREATE, CHICKEN_SALE_UPDATE,
                CHICKEN_SALE_CONFIRM, CHICKEN_SALE_CANCEL,
                PROCUREMENT_READ, PROCUREMENT_CREATE
        );

        put(Role.FARM_WORKER,
                FARM_READ, FARM_UPDATE, NOTIFICATIONS_READ, SETTINGS_READ, CHICKEN_SALE_READ
        );

        put(Role.VETERINARIAN,
                VETERINARY_READ, VETERINARY_CREATE, VETERINARY_UPDATE, VETERINARY_PRESCRIBE,
                FARM_READ, REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ
        );

        put(Role.INVENTORY_MANAGER,
                INVENTORY_READ, INVENTORY_CREATE, INVENTORY_UPDATE,
                INVENTORY_STOCK_IN, INVENTORY_STOCK_OUT, INVENTORY_TRANSFER, INVENTORY_PO,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ,
                // Warehouse procurement ops — Finance owns invoice approve & payment
                PROCUREMENT_READ, PROCUREMENT_CREATE, PROCUREMENT_UPDATE,
                PROCUREMENT_RECEIVE, PROCUREMENT_APPROVE, PROCUREMENT_CANCEL, PROCUREMENT_REPORT
        );

        // Pharmacist: pharmacy ops + inventory READ only (no warehouse admin)
        put(Role.PHARMACIST,
                PHARMACY_READ, PHARMACY_SALE, PHARMACY_DISPENSE, PHARMACY_RETURN,
                INVENTORY_READ, NOTIFICATIONS_READ, SETTINGS_READ, REPORTS_READ
        );

        put(Role.CRM_MANAGER,
                CRM_READ, CRM_CREATE, CRM_UPDATE,
                CRM_LEAD, CRM_LEAD_CONVERT, CRM_OPPORTUNITY,
                CRM_QUOTATION, CRM_QUOTATION_APPROVE,
                CRM_ORDER, CRM_ORDER_CONFIRM, CRM_ORDER_INVOICE, CRM_ORDER_PAY,
                CRM_TASK, CRM_REPORT,
                CONSULTANCY_READ, CONSULTANCY_CREATE, CONSULTANCY_UPDATE, CONSULTANCY_ASSIGN,
                TRAINING_READ,
                REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ,
                CHICKEN_SALE_READ, CHICKEN_SALE_CREATE, FARM_READ,
                EXECUTIVE_DASHBOARD, EXECUTIVE_SALES
        );

        put(Role.MARKETING_MANAGER,
                MARKETING_READ, MARKETING_CREATE, MARKETING_UPDATE,
                CRM_READ, REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ
        );

        put(Role.TRAINING_COORDINATOR,
                TRAINING_READ, TRAINING_CREATE, TRAINING_UPDATE, TRAINING_PUBLISH,
                TRAINING_REGISTER, TRAINING_ATTENDANCE, TRAINING_COMPLETE,
                TRAINING_CERTIFICATE, TRAINING_CANCEL, TRAINING_REPORT,
                CRM_READ, REPORTS_READ, NOTIFICATIONS_READ, SETTINGS_READ
        );

        put(Role.CONSULTANT,
                CONSULTANCY_READ, CONSULTANCY_CREATE, CONSULTANCY_UPDATE,
                CONSULTANCY_ACTIVITY, CONSULTANCY_DELIVERABLE,
                CRM_READ, NOTIFICATIONS_READ, SETTINGS_READ
        );

        put(Role.EXTENSION_WORKER,
                CRM_READ, CRM_UPDATE, CRM_LEAD, CRM_TASK, FARM_READ, TRAINING_READ,
                NOTIFICATIONS_READ, SETTINGS_READ
        );

        // Every canonical role may read its own HR record. Seeing anyone else's still needs HR_READ.
        for (Role role : canonicalRoles()) {
            Set<String> withSelf = new LinkedHashSet<>(MATRIX.getOrDefault(role, Set.of()));
            withSelf.add(HR_SELF);
            MATRIX.put(role, Collections.unmodifiableSet(withSelf));
        }

        // Legacy aliases share the canonical matrix via RoleCompatibility.normalize()
        for (Role legacy : RoleCompatibility.LEGACY_ALIASES) {
            Role canonical = RoleCompatibility.normalize(legacy);
            MATRIX.put(legacy, MATRIX.getOrDefault(canonical, Set.of()));
        }
    }

    private static void put(Role role, String... permissions) {
        Set<String> set = new LinkedHashSet<>();
        Collections.addAll(set, permissions);
        MATRIX.put(role, Collections.unmodifiableSet(set));
    }

    public static List<String> permissionsFor(Role role) {
        Role canonical = RoleCompatibility.normalize(role);
        Set<String> perms = MATRIX.get(canonical);
        if (perms == null || perms.isEmpty()) {
            return List.of();
        }
        return List.copyOf(perms);
    }

    public static boolean hasPermission(Role role, String permission) {
        return permissionsFor(role).contains(permission);
    }

    /** All canonical product roles (excludes deprecated aliases). */
    public static List<Role> canonicalRoles() {
        return List.of(
                Role.ADMIN,
                Role.GENERAL_MANAGER,
                Role.OPERATIONS_MANAGER,
                Role.SECRETARY,
                Role.FINANCE,
                Role.HR,
                Role.FARM_MANAGER,
                Role.FARM_WORKER,
                Role.VETERINARIAN,
                Role.INVENTORY_MANAGER,
                Role.PHARMACIST,
                Role.CRM_MANAGER,
                Role.MARKETING_MANAGER,
                Role.TRAINING_COORDINATOR,
                Role.CONSULTANT,
                Role.EXTENSION_WORKER
        );
    }

    private RolePermissionCatalog() {}
}
