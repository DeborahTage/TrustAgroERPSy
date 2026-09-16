package com.trustagro.crm.dto;

import lombok.Data;

@Data
public class LeadConvertRequest {
    /** Link to an existing CrmClient instead of creating one. */
    private Long existingClientId;
    /** When true, create a new client even if duplicates exist (still requires no existingClientId conflict). */
    private boolean forceCreate;
    /** Explicit confirmation that duplicate candidates were reviewed. */
    private boolean confirmDuplicate;
    private String opportunityTitle;
}
