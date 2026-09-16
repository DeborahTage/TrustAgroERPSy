package com.trustagro.hr.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.hr.entity.PayrollPeriodStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Payroll period lifecycle (backend-authoritative).
 * <pre>
 * OPEN → CALCULATED → APPROVED → PROCESSED → CLOSED
 * OPEN | CALCULATED → CANCELLED
 * </pre>
 * Recalculation is allowed while a period is still OPEN or CALCULATED; once
 * APPROVED the amounts are frozen, and PROCESSED/CLOSED periods are read-only.
 */
public final class PayrollPeriodLifecycle {

    private static final Map<PayrollPeriodStatus, Set<PayrollPeriodStatus>> ALLOWED =
            new EnumMap<>(PayrollPeriodStatus.class);

    static {
        ALLOWED.put(PayrollPeriodStatus.OPEN, EnumSet.of(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.CANCELLED));
        ALLOWED.put(PayrollPeriodStatus.CALCULATED, EnumSet.of(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.APPROVED, PayrollPeriodStatus.CANCELLED));
        ALLOWED.put(PayrollPeriodStatus.APPROVED, EnumSet.of(PayrollPeriodStatus.PROCESSED));
        ALLOWED.put(PayrollPeriodStatus.PROCESSED, EnumSet.of(PayrollPeriodStatus.CLOSED));
        ALLOWED.put(PayrollPeriodStatus.CLOSED, EnumSet.noneOf(PayrollPeriodStatus.class));
        ALLOWED.put(PayrollPeriodStatus.CANCELLED, EnumSet.noneOf(PayrollPeriodStatus.class));
    }

    private PayrollPeriodLifecycle() {}

    public static void assertTransition(PayrollPeriodStatus from, PayrollPeriodStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid payroll period status transition: " + from + " → " + to);
        }
    }

    /** Whether a fresh calculation may replace the current run. */
    public static boolean isCalculable(PayrollPeriodStatus status) {
        return status == PayrollPeriodStatus.OPEN || status == PayrollPeriodStatus.CALCULATED;
    }

    /** Whether payroll amounts, payslips and salary effects for this period are frozen. */
    public static boolean isLocked(PayrollPeriodStatus status) {
        return status == PayrollPeriodStatus.PROCESSED || status == PayrollPeriodStatus.CLOSED
                || status == PayrollPeriodStatus.CANCELLED;
    }

    /** Payments may only be disbursed once payroll has been posted to Finance. */
    public static boolean isPayable(PayrollPeriodStatus status) {
        return status == PayrollPeriodStatus.PROCESSED || status == PayrollPeriodStatus.CLOSED;
    }
}
