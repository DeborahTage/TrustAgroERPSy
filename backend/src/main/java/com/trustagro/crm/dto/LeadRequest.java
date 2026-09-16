package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmLeadSource;
import com.trustagro.crm.entity.CrmPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeadRequest {
    @NotBlank @Size(max = 160) private String name;
    @Size(max = 160) private String company;
    @Size(max = 120) private String contactPerson;
    @Size(max = 40) private String phone;
    @Size(max = 120) private String email;
    private CrmLeadSource source;
    @Size(max = 80) private String industry;
    private String notes;
    private Long assignedOwnerId;
    private CrmPriority priority;
    private Long marketingLeadId;
}
