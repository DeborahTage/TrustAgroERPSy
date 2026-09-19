package com.trustagro.farm.delivery.dto;

import lombok.Data;

@Data
public class MarketingLogisticsRequest {
    private String marketingNotes;
    private String deliveryOutcome;
    /** MARKETING_RECEIVED, OUT_FOR_DELIVERY, DELIVERED, DELIVERY_FAILED */
    private String logisticsStatus;
}
