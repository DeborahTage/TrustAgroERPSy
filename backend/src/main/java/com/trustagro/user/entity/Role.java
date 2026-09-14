package com.trustagro.user.entity;

/**
 * Product role vocabulary. Prefer canonical values in {@link #canonical()} sense.
 * Deprecated constants exist only so existing DB rows deserialize until DataSeeder migrates them.
 */
public enum Role {
    // Canonical product roles
    ADMIN,
    GENERAL_MANAGER,
    OPERATIONS_MANAGER,
    SECRETARY,
    FINANCE,
    HR,
    FARM_MANAGER,
    FARM_WORKER,
    VETERINARIAN,
    INVENTORY_MANAGER,
    PHARMACIST,
    CRM_MANAGER,
    MARKETING_MANAGER,
    TRAINING_COORDINATOR,
    CONSULTANT,
    EXTENSION_WORKER,

    /** @deprecated Use {@link #MARKETING_MANAGER} */
    @Deprecated MARKETING,
    /** @deprecated Use {@link #TRAINING_COORDINATOR} */
    @Deprecated TRAINING_OFFICER,
    /** @deprecated Use {@link #VETERINARIAN} */
    @Deprecated VETERINARY_OFFICER,
    /** @deprecated Use {@link #INVENTORY_MANAGER} */
    @Deprecated STORE_KEEPER,
    /** @deprecated Use {@link #PHARMACIST} */
    @Deprecated PHARMACY_SALES,
    /** @deprecated Use {@link #FINANCE} */
    @Deprecated FINANCE_OFFICER
}
