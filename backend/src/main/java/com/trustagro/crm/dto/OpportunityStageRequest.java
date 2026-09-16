package com.trustagro.crm.dto;

import com.trustagro.crm.entity.OpportunityStage;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpportunityStageRequest {
    @NotNull private OpportunityStage stage;
    private String lossReason;
}
