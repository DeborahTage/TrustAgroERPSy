package com.trustagro.hr.entity;

/**
 * Buckets a deduction type into the payroll item column it accumulates into.
 * Rates themselves are configuration, never hard-coded statutory rules.
 */
public enum DeductionCategory {
    INCOME_TAX, PENSION, OTHER
}
