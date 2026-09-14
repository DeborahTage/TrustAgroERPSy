package com.trustagro.security;

import com.trustagro.user.entity.Role;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps legacy / deprecated role enum values onto the canonical product role set.
 * Used at auth time so JWT authorities and permission resolution stay consistent
 * even if a DB row has not yet been migrated.
 */
public final class RoleCompatibility {

    private RoleCompatibility() {}

    /** Roles that remain only for DB/enum deserialization until rows are migrated. */
    public static final Set<Role> LEGACY_ALIASES = EnumSet.of(
            Role.STORE_KEEPER,
            Role.PHARMACY_SALES,
            Role.FINANCE_OFFICER,
            Role.VETERINARY_OFFICER,
            Role.MARKETING,
            Role.TRAINING_OFFICER
    );

    private static final Map<Role, Role> ALIASES = Map.of(
            Role.STORE_KEEPER, Role.INVENTORY_MANAGER,
            Role.PHARMACY_SALES, Role.PHARMACIST,
            Role.FINANCE_OFFICER, Role.FINANCE,
            Role.VETERINARY_OFFICER, Role.VETERINARIAN,
            Role.MARKETING, Role.MARKETING_MANAGER,
            Role.TRAINING_OFFICER, Role.TRAINING_COORDINATOR
    );

    public static Role normalize(Role role) {
        if (role == null) return null;
        return ALIASES.getOrDefault(role, role);
    }

    public static boolean isCanonical(Role role) {
        if (role == null) return false;
        return !LEGACY_ALIASES.contains(role);
    }
}
