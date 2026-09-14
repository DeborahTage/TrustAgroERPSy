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
}
