package com.trustagro.farm.sale.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.sale.entity.ChickenSaleStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Canonical chicken-sale state machine. The backend is the only authority
 * for transitions; the frontend may request an action, never a raw status.
 *
 * <pre>
 * DRAFT → CONFIRMED → INVOICED → PARTIALLY_PAID → PAID → COMPLETED
 *   └──────────────┘ → CANCELLED (only before invoice)
 * </pre>
 *
 * Who may request each transition (enforced by controller RBAC):
 * <ul>
 *   <li>CREATE / UPDATE (DRAFT only): chickensale:create / chickensale:update — FARM_MANAGER, CRM_MANAGER (create)</li>
 *   <li>CONFIRM: chickensale:confirm — FARM_MANAGER, OPERATIONS_MANAGER (decrements flock)</li>
 *   <li>INVOICE: chickensale:invoice — FINANCE</li>
 *   <li>PAYMENT: chickensale:pay — FINANCE</li>
 *   <li>COMPLETE: chickensale:confirm — FARM_MANAGER, OPERATIONS_MANAGER</li>
 *   <li>CANCEL: chickensale:cancel — FARM_MANAGER (DRAFT or CONFIRMED only)</li>
 * </ul>
 */
public final class ChickenSaleLifecycle {

    private static final Map<ChickenSaleStatus, Set<ChickenSaleStatus>> ALLOWED = new EnumMap<>(ChickenSaleStatus.class);

    static {
        ALLOWED.put(ChickenSaleStatus.DRAFT, EnumSet.of(ChickenSaleStatus.CONFIRMED, ChickenSaleStatus.CANCELLED));
        ALLOWED.put(ChickenSaleStatus.CONFIRMED, EnumSet.of(ChickenSaleStatus.INVOICED, ChickenSaleStatus.CANCELLED));
        ALLOWED.put(ChickenSaleStatus.INVOICED, EnumSet.of(ChickenSaleStatus.PARTIALLY_PAID, ChickenSaleStatus.PAID));
        ALLOWED.put(ChickenSaleStatus.PARTIALLY_PAID, EnumSet.of(ChickenSaleStatus.PARTIALLY_PAID, ChickenSaleStatus.PAID));
        ALLOWED.put(ChickenSaleStatus.PAID, EnumSet.of(ChickenSaleStatus.COMPLETED));
        ALLOWED.put(ChickenSaleStatus.COMPLETED, EnumSet.noneOf(ChickenSaleStatus.class));
        ALLOWED.put(ChickenSaleStatus.CANCELLED, EnumSet.noneOf(ChickenSaleStatus.class));
    }

    private ChickenSaleLifecycle() {}

    public static boolean isAllowed(ChickenSaleStatus from, ChickenSaleStatus to) {
        if (from == null || to == null) return false;
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransition(ChickenSaleStatus from, ChickenSaleStatus to) {
        if (!isAllowed(from, to)) {
            throw new BusinessException("Invalid chicken sale status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(ChickenSaleStatus status) {
        return status == ChickenSaleStatus.DRAFT;
    }

    public static boolean isCancellable(ChickenSaleStatus status) {
        return status == ChickenSaleStatus.DRAFT || status == ChickenSaleStatus.CONFIRMED;
    }
}
