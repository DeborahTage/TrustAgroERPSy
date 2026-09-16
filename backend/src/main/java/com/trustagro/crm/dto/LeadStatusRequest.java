package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmLeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeadStatusRequest {
    @NotNull private CrmLeadStatus status;
}
