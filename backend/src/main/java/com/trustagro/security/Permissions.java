package com.trustagro.security;

/**
 * Canonical permission vocabulary: {@code MODULE:ACTION}.
 * Backend {@code @PreAuthorize(hasAuthority(...))} and frontend checks must use these strings.
 */
public final class Permissions {

    private Permissions() {}

    // Admin / system
    public static final String USERS_READ = "users:read";
    public static final String USERS_CREATE = "users:create";
    public static final String USERS_UPDATE = "users:update";
    public static final String USERS_DELETE = "users:delete";
    public static final String ROLES_MANAGE = "roles:manage";
    public static final String SETTINGS_READ = "settings:read";
    public static final String SETTINGS_UPDATE = "settings:update";
    public static final String AUDIT_READ = "audit:read";
    public static final String REPORTS_READ = "reports:read";
    public static final String NOTIFICATIONS_READ = "notifications:read";
    public static final String MASTERDATA_READ = "masterdata:read";
    public static final String MASTERDATA_UPDATE = "masterdata:update";

    // Farm
    public static final String FARM_READ = "farm:read";
    public static final String FARM_CREATE = "farm:create";
    public static final String FARM_UPDATE = "farm:update";
    public static final String FARM_DELETE = "farm:delete";

    // Chicken sales (canonical commercial workflow)
    public static final String CHICKEN_SALE_READ = "chickensale:read";
    public static final String CHICKEN_SALE_CREATE = "chickensale:create";
    public static final String CHICKEN_SALE_UPDATE = "chickensale:update";
    public static final String CHICKEN_SALE_CONFIRM = "chickensale:confirm";
    public static final String CHICKEN_SALE_CANCEL = "chickensale:cancel";
    public static final String CHICKEN_SALE_INVOICE = "chickensale:invoice";
    public static final String CHICKEN_SALE_PAY = "chickensale:pay";

    // Veterinary
    public static final String VETERINARY_READ = "veterinary:read";
    public static final String VETERINARY_CREATE = "veterinary:create";
    public static final String VETERINARY_UPDATE = "veterinary:update";
    public static final String VETERINARY_PRESCRIBE = "veterinary:prescribe";

    // Inventory
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_CREATE = "inventory:create";
    public static final String INVENTORY_UPDATE = "inventory:update";
    public static final String INVENTORY_STOCK_IN = "inventory:stock_in";
    public static final String INVENTORY_STOCK_OUT = "inventory:stock_out";
    public static final String INVENTORY_TRANSFER = "inventory:transfer";
    public static final String INVENTORY_PO = "inventory:po";

    // Pharmacy
    public static final String PHARMACY_READ = "pharmacy:read";
    public static final String PHARMACY_SALE = "pharmacy:sale";
    public static final String PHARMACY_DISPENSE = "pharmacy:dispense";
    public static final String PHARMACY_RETURN = "pharmacy:return";

    // Finance
    public static final String FINANCE_READ = "finance:read";
    public static final String FINANCE_CREATE = "finance:create";
    public static final String FINANCE_APPROVE = "finance:approve";
    public static final String FINANCE_RECONCILE = "finance:reconcile";

    // CRM
    public static final String CRM_READ = "crm:read";
    public static final String CRM_CREATE = "crm:create";
    public static final String CRM_UPDATE = "crm:update";

    // Marketing
    public static final String MARKETING_READ = "marketing:read";
    public static final String MARKETING_CREATE = "marketing:create";
    public static final String MARKETING_UPDATE = "marketing:update";

    // Training
    public static final String TRAINING_READ = "training:read";
    public static final String TRAINING_CREATE = "training:create";
    public static final String TRAINING_UPDATE = "training:update";
    public static final String TRAINING_PUBLISH = "training:publish";
    public static final String TRAINING_REGISTER = "training:register";
    public static final String TRAINING_ATTENDANCE = "training:attendance";
    public static final String TRAINING_COMPLETE = "training:complete";
    public static final String TRAINING_CERTIFICATE = "training:certificate";
    public static final String TRAINING_PAYMENT = "training:payment";
    public static final String TRAINING_REPORT = "training:report";
    public static final String TRAINING_CANCEL = "training:cancel";

    // Consultancy
    public static final String CONSULTANCY_READ = "consultancy:read";
    public static final String CONSULTANCY_CREATE = "consultancy:create";
    public static final String CONSULTANCY_UPDATE = "consultancy:update";

    // HR
    public static final String HR_READ = "hr:read";
    public static final String HR_CREATE = "hr:create";
    public static final String HR_UPDATE = "hr:update";
}
