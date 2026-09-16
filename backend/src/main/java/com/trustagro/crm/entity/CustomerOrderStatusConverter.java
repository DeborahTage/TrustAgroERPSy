package com.trustagro.crm.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class CustomerOrderStatusConverter implements AttributeConverter<CustomerOrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(CustomerOrderStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public CustomerOrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return CustomerOrderStatus.DRAFT;
        }
        return switch (dbData.trim().toUpperCase()) {
            case "PENDING", "DRAFT" -> CustomerOrderStatus.DRAFT;
            case "CONFIRMED" -> CustomerOrderStatus.CONFIRMED;
            case "SHIPPED", "IN_PROGRESS" -> CustomerOrderStatus.IN_PROGRESS;
            case "PARTIALLY_FULFILLED" -> CustomerOrderStatus.PARTIALLY_FULFILLED;
            case "DELIVERED", "FULFILLED" -> CustomerOrderStatus.FULFILLED;
            case "CANCELLED" -> CustomerOrderStatus.CANCELLED;
            default -> {
                try {
                    yield CustomerOrderStatus.valueOf(dbData.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    yield CustomerOrderStatus.DRAFT;
                }
            }
        };
    }
}
