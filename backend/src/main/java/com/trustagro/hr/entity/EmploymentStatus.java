package com.trustagro.hr.entity;

public enum EmploymentStatus {
    ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED, INACTIVE;

    /** Only these statuses are picked up by a payroll calculation run. */
    public boolean isPayrollEligible() {
        return this == ACTIVE || this == ON_LEAVE;
    }
}
