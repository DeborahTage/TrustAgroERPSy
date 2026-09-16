package com.trustagro.inventory.entity;

/**
 * Purchase order lifecycle.
 * Legacy DB values Draft/Sent/Received are mapped via {@link PurchaseOrderStatusConverter}.
 */
public enum PurchaseOrderStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    SENT,
    PARTIALLY_RECEIVED,
    FULLY_RECEIVED,
    CANCELLED,
    CLOSED
}
