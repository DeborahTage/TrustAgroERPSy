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
    public static final String GOVERNANCE_READ = "governance:read";
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
    /** Finance payment-verification / release authorization for chicken delivery. */
    public static final String CHICKEN_SALE_VERIFY = "chickensale:verify";

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

    // Procurement / AP
    public static final String PROCUREMENT_READ = "procurement:read";
    public static final String PROCUREMENT_CREATE = "procurement:create";
    public static final String PROCUREMENT_UPDATE = "procurement:update";
    public static final String PROCUREMENT_APPROVE = "procurement:approve";
    public static final String PROCUREMENT_RECEIVE = "procurement:receive";
    public static final String PROCUREMENT_INVOICE = "procurement:invoice";
    public static final String PROCUREMENT_PAYMENT = "procurement:payment";
    public static final String PROCUREMENT_REPORT = "procurement:report";
    public static final String PROCUREMENT_CANCEL = "procurement:cancel";

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
    public static final String CRM_LEAD = "crm:lead";
    public static final String CRM_LEAD_CONVERT = "crm:lead_convert";
    public static final String CRM_OPPORTUNITY = "crm:opportunity";
    public static final String CRM_QUOTATION = "crm:quotation";
    public static final String CRM_QUOTATION_APPROVE = "crm:quotation_approve";
    public static final String CRM_ORDER = "crm:order";
    public static final String CRM_ORDER_CONFIRM = "crm:order_confirm";
    public static final String CRM_ORDER_INVOICE = "crm:order_invoice";
    public static final String CRM_ORDER_PAY = "crm:order_pay";
    public static final String CRM_TASK = "crm:task";
    public static final String CRM_REPORT = "crm:report";

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
    public static final String CONSULTANCY_ASSIGN = "consultancy:assign";
    public static final String CONSULTANCY_APPROVE = "consultancy:approve";
    public static final String CONSULTANCY_COMPLETE = "consultancy:complete";
    public static final String CONSULTANCY_ACTIVITY = "consultancy:activity";
    public static final String CONSULTANCY_DELIVERABLE = "consultancy:deliverable";
    public static final String CONSULTANCY_CONTRACT = "consultancy:contract";
    public static final String CONSULTANCY_INVOICE = "consultancy:invoice";
    public static final String CONSULTANCY_PAYMENT = "consultancy:payment";
    public static final String CONSULTANCY_REPORT = "consultancy:report";
    public static final String CONSULTANCY_CANCEL = "consultancy:cancel";

    // HR
    public static final String HR_READ = "hr:read";
    public static final String HR_CREATE = "hr:create";
    public static final String HR_UPDATE = "hr:update";
    public static final String HR_TERMINATE = "hr:terminate";
    public static final String HR_ATTENDANCE = "hr:attendance";
    public static final String HR_LEAVE = "hr:leave";
    public static final String HR_LEAVE_APPROVE = "hr:leave_approve";
    public static final String HR_PAYROLL = "hr:payroll";
    public static final String HR_PAYROLL_APPROVE = "hr:payroll_approve";
    public static final String HR_PAYROLL_PROCESS = "hr:payroll_process";
    public static final String HR_REPORT = "hr:report";
    public static final String HR_SELF = "hr:self";

    // Material requests (Step 17.2)
    public static final String MATERIAL_REQUEST_READ = "material_request:read";
    public static final String MATERIAL_REQUEST_CREATE = "material_request:create";
    public static final String MATERIAL_REQUEST_UPDATE = "material_request:update";
    public static final String MATERIAL_REQUEST_SUBMIT = "material_request:submit";
    public static final String MATERIAL_REQUEST_APPROVE = "material_request:approve";
    public static final String MATERIAL_REQUEST_FULFILL = "material_request:fulfill";
    public static final String MATERIAL_REQUEST_RECEIVE = "material_request:receive";
    public static final String MATERIAL_REQUEST_REPORT = "material_request:report";

    // Executive (read-only cross-module aggregation)
    public static final String EXECUTIVE_DASHBOARD = "executive:dashboard";
    public static final String EXECUTIVE_FINANCE = "executive:finance";
    public static final String EXECUTIVE_SALES = "executive:sales";
    public static final String EXECUTIVE_FARM = "executive:farm";
    public static final String EXECUTIVE_INVENTORY = "executive:inventory";
    public static final String EXECUTIVE_HR = "executive:hr";
    public static final String EXECUTIVE_REPORT = "executive:report";
}
