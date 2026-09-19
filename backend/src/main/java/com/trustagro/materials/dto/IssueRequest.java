package com.trustagro.materials.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class IssueRequest {
    private String idempotencyKey;
    private String notes;

    @NotEmpty
    @Valid
    private List<LineIssue> lines;

    @Data
    public static class LineIssue {
        private Long lineId;
        private BigDecimal quantityIssued;
    }
}
