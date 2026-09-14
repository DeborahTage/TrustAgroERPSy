package com.trustagro.finance.entity;

public enum AccountCode {
    // Revenue Accounts
    REV_POULTRY_SALES("4100", "Poultry Sales", AccountType.REVENUE, BusinessUnit.POULTRY_FARM),
    REV_EGG_SALES("4110", "Egg Sales", AccountType.REVENUE, BusinessUnit.POULTRY_FARM),
    REV_PHARMACY_DRUGS("4200", "Pharmacy Drug Sales", AccountType.REVENUE, BusinessUnit.PHARMACY),
    REV_PHARMACY_VACCINES("4210", "Pharmacy Vaccine Sales", AccountType.REVENUE, BusinessUnit.PHARMACY),
    REV_PHARMACY_EQUIPMENT("4220", "Pharmacy Equipment Sales", AccountType.REVENUE, BusinessUnit.PHARMACY),
    REV_VET_SERVICES("4300", "Veterinary Services", AccountType.REVENUE, BusinessUnit.PHARMACY),
    REV_CONSULTING("4400", "Consulting Revenue", AccountType.REVENUE, BusinessUnit.CONSULTING),
    REV_TRAINING("4500", "Training Revenue", AccountType.REVENUE, BusinessUnit.TRAINING),
    REV_WORKSHOP("4510", "Workshop Revenue", AccountType.REVENUE, BusinessUnit.TRAINING),
    REV_OTHER("4900", "Other Revenue", AccountType.REVENUE, BusinessUnit.GENERAL),

    // Expense Accounts
    EXP_FEED("5100", "Feed Expenses", AccountType.EXPENSE, BusinessUnit.POULTRY_FARM),
    EXP_VACCINE("5110", "Vaccine Expenses", AccountType.EXPENSE, BusinessUnit.POULTRY_FARM),
    EXP_DRUG("5120", "Drug Expenses", AccountType.EXPENSE, BusinessUnit.POULTRY_FARM),
    EXP_FARM_LABOR("5200", "Farm Labor", AccountType.EXPENSE, BusinessUnit.POULTRY_FARM),
    EXP_UTILITIES("5300", "Utilities", AccountType.EXPENSE, BusinessUnit.GENERAL),
    EXP_TRANSPORT("5400", "Transport", AccountType.EXPENSE, BusinessUnit.GENERAL),
    EXP_TRAINING_COSTS("5500", "Training Costs", AccountType.EXPENSE, BusinessUnit.TRAINING),
    EXP_CONSULTING_COSTS("5600", "Consulting Costs", AccountType.EXPENSE, BusinessUnit.CONSULTING),
    EXP_MARKETING("5700", "Marketing", AccountType.EXPENSE, BusinessUnit.GENERAL),
    EXP_EQUIPMENT("5800", "Equipment", AccountType.EXPENSE, BusinessUnit.GENERAL),
    EXP_MAINTENANCE("5900", "Maintenance", AccountType.EXPENSE, BusinessUnit.GENERAL),
    EXP_ADMIN("5950", "Administrative Costs", AccountType.EXPENSE, BusinessUnit.GENERAL),
    COGS_POULTRY("5000", "Cost of Goods Sold - Poultry", AccountType.EXPENSE, BusinessUnit.POULTRY_FARM),
    COGS_PHARMACY("5010", "Cost of Goods Sold - Pharmacy", AccountType.EXPENSE, BusinessUnit.PHARMACY),

    // Asset Accounts
    ASSET_CASH("1000", "Cash on Hand", AccountType.ASSET, BusinessUnit.GENERAL),
    ASSET_BANK("1010", "Bank Account", AccountType.ASSET, BusinessUnit.GENERAL),
    ASSET_RECEIVABLE("1200", "Accounts Receivable", AccountType.ASSET, BusinessUnit.GENERAL),
    ASSET_WALLET("1300", "Customer Wallets", AccountType.ASSET, BusinessUnit.GENERAL),
    ASSET_INVENTORY("1400", "Inventory", AccountType.ASSET, BusinessUnit.GENERAL),

    // Liability Accounts
    LIABILITY_VAT("2100", "VAT Payable", AccountType.LIABILITY, BusinessUnit.GENERAL),
    LIABILITY_AP("2000", "Accounts Payable", AccountType.LIABILITY, BusinessUnit.GENERAL),
    LIABILITY_CREDIT("2200", "Customer Credit Payable", AccountType.LIABILITY, BusinessUnit.GENERAL);

    private final String code;
    private final String name;
    private final AccountType accountType;
    private final BusinessUnit unit;

    AccountCode(String code, String name, AccountType accountType, BusinessUnit unit) {
        this.code = code;
        this.name = name;
        this.accountType = accountType;
        this.unit = unit;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public AccountType getAccountType() { return accountType; }
    public BusinessUnit getUnit() { return unit; }
}
