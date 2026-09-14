package com.trustagro.veterinary.dto;

import lombok.Data;

@Data
public class DispensePrescriptionRequest {
    /** Optional explicit batch; when null, FEFO selects automatically. */
    private Long batchId;
    private String notes;
}
