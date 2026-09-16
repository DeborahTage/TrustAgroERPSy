package com.trustagro.procurement.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * DRAFT → SUBMITTED → MATCHED|MISMATCHED → APPROVED → PARTIALLY_PAID → PAID
 * MISMATCHED → SUBMITTED (re-match after correction)
 * DRAFT/SUBMITTED/MATCHED/MISMATCHED → CANCELLED
 * No PAID → APPROVED.
 * </pre>
 */
public final class SupplierInvoiceLifecycle {

    private static final Map<SupplierInvoiceStatus, Set<SupplierInvoiceStatus>> ALLOWED = new EnumMap<>(SupplierInvoiceStatus.class);

    static {
        ALLOWED.put(SupplierInvoiceStatus.DRAFT, EnumSet.of(
                SupplierInvoiceStatus.SUBMITTED, SupplierInvoiceStatus.CANCELLED));
        ALLOWED.put(SupplierInvoiceStatus.SUBMITTED, EnumSet.of(
                SupplierInvoiceStatus.MATCHED, SupplierInvoiceStatus.MISMATCHED,
                SupplierInvoiceStatus.CANCELLED));
        ALLOWED.put(SupplierInvoiceStatus.MATCHED, EnumSet.of(
                SupplierInvoiceStatus.APPROVED, SupplierInvoiceStatus.CANCELLED));
        ALLOWED.put(SupplierInvoiceStatus.MISMATCHED, EnumSet.of(
                SupplierInvoiceStatus.SUBMITTED, SupplierInvoiceStatus.MATCHED,
                SupplierInvoiceStatus.MISMATCHED, SupplierInvoiceStatus.CANCELLED));
        ALLOWED.put(SupplierInvoiceStatus.APPROVED, EnumSet.of(
                SupplierInvoiceStatus.PARTIALLY_PAID, SupplierInvoiceStatus.PAID));
        ALLOWED.put(SupplierInvoiceStatus.PARTIALLY_PAID, EnumSet.of(
                SupplierInvoiceStatus.PARTIALLY_PAID, SupplierInvoiceStatus.PAID));
        ALLOWED.put(SupplierInvoiceStatus.PAID, EnumSet.noneOf(SupplierInvoiceStatus.class));
        ALLOWED.put(SupplierInvoiceStatus.CANCELLED, EnumSet.noneOf(SupplierInvoiceStatus.class));
    }

    private SupplierInvoiceLifecycle() {}

    public static void assertTransition(SupplierInvoiceStatus from, SupplierInvoiceStatus to) {
        if (from == to && (from == SupplierInvoiceStatus.PARTIALLY_PAID || from == SupplierInvoiceStatus.MISMATCHED)) {
            return;
        }
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid supplier invoice status transition: " + from + " → " + to);
        }
    }
}
