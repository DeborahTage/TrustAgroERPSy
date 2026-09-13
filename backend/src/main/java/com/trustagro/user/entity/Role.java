package com.trustagro.user.entity;

public enum Role {
    ADMIN,
    FARM_MANAGER,
    INVENTORY_MANAGER,
    PHARMACIST,
    VETERINARIAN,
    FINANCE,
    CRM_MANAGER,
    MARKETING,
    TRAINING_OFFICER,
    // Legacy — kept for DB migration compatibility
    GENERAL_MANAGER,
    OPERATIONS_MANAGER,
    VETERINARY_OFFICER,
    STORE_KEEPER,
    PHARMACY_SALES,
    FINANCE_OFFICER,
    EXTENSION_WORKER
}
