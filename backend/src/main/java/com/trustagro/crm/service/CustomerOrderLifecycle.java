package com.trustagro.crm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.CustomerOrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Customer order lifecycle.
 * <pre>
 * DRAFT → CONFIRMED → IN_PROGRESS → PARTIALLY_FULFILLED → FULFILLED
 *   └───────┴──────────┴────────────┴────────────────────→ CANCELLED
 * </pre>
 */
public final class CustomerOrderLifecycle {

    private static final Map<CustomerOrderStatus, Set<CustomerOrderStatus>> ALLOWED =
            new EnumMap<>(CustomerOrderStatus.class);

    static {
        ALLOWED.put(CustomerOrderStatus.DRAFT, EnumSet.of(
                CustomerOrderStatus.CONFIRMED, CustomerOrderStatus.CANCELLED));
        ALLOWED.put(CustomerOrderStatus.CONFIRMED, EnumSet.of(
                CustomerOrderStatus.IN_PROGRESS, CustomerOrderStatus.FULFILLED, CustomerOrderStatus.CANCELLED));
        ALLOWED.put(CustomerOrderStatus.IN_PROGRESS, EnumSet.of(
                CustomerOrderStatus.PARTIALLY_FULFILLED, CustomerOrderStatus.FULFILLED, CustomerOrderStatus.CANCELLED));
        ALLOWED.put(CustomerOrderStatus.PARTIALLY_FULFILLED, EnumSet.of(
                CustomerOrderStatus.FULFILLED, CustomerOrderStatus.CANCELLED));
        ALLOWED.put(CustomerOrderStatus.FULFILLED, EnumSet.noneOf(CustomerOrderStatus.class));
        ALLOWED.put(CustomerOrderStatus.CANCELLED, EnumSet.noneOf(CustomerOrderStatus.class));
    }

    private CustomerOrderLifecycle() {}

    public static void assertTransition(CustomerOrderStatus from, CustomerOrderStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid order status transition: " + from + " → " + to);
        }
    }
}
