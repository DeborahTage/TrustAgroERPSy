package com.trustagro.finance.entity;

/**
 * Canonical finance source / posting-purpose vocabulary.
 * {@code FinanceTransaction.sourceModule} identifies the originating module.
 * {@code FinanceTransaction.referenceType} identifies the posting purpose (must be unique per sourceId).
 * {@code FinanceTransaction.referenceId} points at the business entity (sale, invoice, batch, etc.).
 */
public final class FinanceSourceTypes {

    private FinanceSourceTypes() {}

    // Modules
    public static final String MODULE_PHARMACY = "PHARMACY";
    public static final String MODULE_CHICKEN_SALE = "CHICKEN_SALE";
    public static final String MODULE_INVENTORY = "INVENTORY";
    public static final String MODULE_VETERINARY = "VETERINARY";
    public static final String MODULE_INVOICE = "INVOICE";
    public static final String MODULE_RECONCILIATION = "RECONCILIATION";
    public static final String MODULE_MANUAL = "MANUAL";
    public static final String MODULE_TRAINING = "TRAINING";
    public static final String MODULE_CONSULTANCY = "CONSULTANCY";
    public static final String MODULE_HR = "HR";
    public static final String MODULE_PROCUREMENT = "PROCUREMENT";
    public static final String MODULE_CRM = "CRM";

    // Posting purposes (referenceType) — one posting per (module, type, id)
    public static final String REF_PHARMACY_SALE_REVENUE = "PHARMACY_SALE_REVENUE";
    public static final String REF_PHARMACY_SALE_VAT = "PHARMACY_SALE_VAT";
    public static final String REF_PHARMACY_SALE_COGS = "PHARMACY_SALE_COGS";

    public static final String REF_INVOICE_REVENUE = "INVOICE_REVENUE";
    public static final String REF_INVOICE_VAT = "INVOICE_VAT";
    public static final String REF_PAYMENT = "PAYMENT";

    public static final String REF_STOCK_IN = "STOCK_IN";
    public static final String REF_STOCK_OUT_COGS = "STOCK_OUT_COGS";
    public static final String REF_INTERNAL_CONSUMPTION = "INTERNAL_CONSUMPTION";
    public static final String REF_REQUISITION = "REQUISITION";
    public static final String REF_VET_DISPENSE = "VETERINARY_PRESCRIPTION";
    public static final String REF_CASH_REC = "CASH_REC";

    public static final String REF_CONSULTANCY_ENGAGEMENT = "CONSULTANCY_ENGAGEMENT";
    public static final String REF_CONSULTANCY_MILESTONE = "CONSULTANCY_MILESTONE";

    /** Invoice sourceType for CRM customer orders (sourceId = order.id). */
    public static final String REF_CRM_ORDER = "CRM_ORDER";
    /** Prefer REF_INVOICE_REVENUE + invoice.id for revenue posting (like consultancy). */
    public static final String REF_CRM_ORDER_INVOICE = "CRM_ORDER_INVOICE";

    /** referenceId = payrollRunId — gross salary expense accrual. */
    public static final String REF_HR_PAYROLL_EXPENSE = "HR_PAYROLL_EXPENSE";
    /** referenceId = payrollRunId — reclass of withheld deductions out of salary payable. */
    public static final String REF_HR_PAYROLL_DEDUCTION = "HR_PAYROLL_DEDUCTION";
    /** referenceId = payrollPaymentId — settlement of salary payable in cash/bank. */
    public static final String REF_HR_PAYROLL_PAYMENT = "HR_PAYROLL_PAYMENT";

    /** referenceId = supplierPaymentId — Dr AP / Cr cash or bank. Never inventory. */
    public static final String REF_PROCUREMENT_PAYMENT = "PROCUREMENT_PAYMENT";
    /**
     * Optional marker for stock movements / audits.
     * Inventory + AP recognition still posts as INVENTORY / STOCK_IN / batchId.
     */
    public static final String REF_PROCUREMENT_GRN = "PROCUREMENT_GRN";
}
