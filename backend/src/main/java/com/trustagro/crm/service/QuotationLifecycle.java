package com.trustagro.crm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.QuotationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Quotation lifecycle.
 * <pre>
 * DRAFT → SUBMITTED → APPROVED → SENT → ACCEPTED
 *   └───────┴──────────┴─────────┴─────→ REJECTED | EXPIRED | CANCELLED
 * </pre>
 */
public final class QuotationLifecycle {

    private static final Map<QuotationStatus, Set<QuotationStatus>> ALLOWED = new EnumMap<>(QuotationStatus.class);

    static {
        ALLOWED.put(QuotationStatus.DRAFT, EnumSet.of(
                QuotationStatus.SUBMITTED, QuotationStatus.CANCELLED));
        ALLOWED.put(QuotationStatus.SUBMITTED, EnumSet.of(
                QuotationStatus.APPROVED, QuotationStatus.REJECTED, QuotationStatus.CANCELLED));
        ALLOWED.put(QuotationStatus.APPROVED, EnumSet.of(
                QuotationStatus.SENT, QuotationStatus.CANCELLED, QuotationStatus.EXPIRED));
        ALLOWED.put(QuotationStatus.SENT, EnumSet.of(
                QuotationStatus.ACCEPTED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED, QuotationStatus.CANCELLED));
        ALLOWED.put(QuotationStatus.ACCEPTED, EnumSet.noneOf(QuotationStatus.class));
        ALLOWED.put(QuotationStatus.REJECTED, EnumSet.noneOf(QuotationStatus.class));
        ALLOWED.put(QuotationStatus.EXPIRED, EnumSet.noneOf(QuotationStatus.class));
        ALLOWED.put(QuotationStatus.CANCELLED, EnumSet.noneOf(QuotationStatus.class));
    }

    private QuotationLifecycle() {}

    public static void assertTransition(QuotationStatus from, QuotationStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid quotation status transition: " + from + " → " + to);
        }
    }
}
