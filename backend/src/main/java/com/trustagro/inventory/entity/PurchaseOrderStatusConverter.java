package com.trustagro.inventory.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PurchaseOrderStatusConverter implements AttributeConverter<PurchaseOrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(PurchaseOrderStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PurchaseOrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PurchaseOrderStatus.DRAFT;
        }
        return switch (dbData.trim()) {
            case "Draft", "DRAFT" -> PurchaseOrderStatus.DRAFT;
            case "Submitted", "SUBMITTED" -> PurchaseOrderStatus.SUBMITTED;
            case "Approved", "APPROVED" -> PurchaseOrderStatus.APPROVED;
            case "Sent", "SENT" -> PurchaseOrderStatus.SENT;
            case "Partially Received", "PARTIALLY_RECEIVED" -> PurchaseOrderStatus.PARTIALLY_RECEIVED;
            case "Received", "Fully Received", "FULLY_RECEIVED" -> PurchaseOrderStatus.FULLY_RECEIVED;
            case "Cancelled", "CANCELLED" -> PurchaseOrderStatus.CANCELLED;
            case "Closed", "CLOSED" -> PurchaseOrderStatus.CLOSED;
            default -> {
                try {
                    yield PurchaseOrderStatus.valueOf(dbData.trim());
                } catch (IllegalArgumentException ex) {
                    yield PurchaseOrderStatus.DRAFT;
                }
            }
        };
    }
}
