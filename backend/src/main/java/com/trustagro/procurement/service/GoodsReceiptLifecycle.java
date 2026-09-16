package com.trustagro.procurement.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.procurement.entity.GoodsReceiptStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * V1: DRAFT → POSTED | CANCELLED. POSTED is terminal (idempotent short-circuit in service).
 */
public final class GoodsReceiptLifecycle {

    private static final Map<GoodsReceiptStatus, Set<GoodsReceiptStatus>> ALLOWED = new EnumMap<>(GoodsReceiptStatus.class);

    static {
        ALLOWED.put(GoodsReceiptStatus.DRAFT, EnumSet.of(
                GoodsReceiptStatus.POSTED, GoodsReceiptStatus.CANCELLED));
        ALLOWED.put(GoodsReceiptStatus.POSTED, EnumSet.noneOf(GoodsReceiptStatus.class));
        ALLOWED.put(GoodsReceiptStatus.CANCELLED, EnumSet.noneOf(GoodsReceiptStatus.class));
    }

    private GoodsReceiptLifecycle() {}

    public static void assertTransition(GoodsReceiptStatus from, GoodsReceiptStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid goods receipt status transition: " + from + " → " + to);
        }
    }
}
