package com.trustagro.consultancy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of idempotent CRM quotation → request → engagement → contract conversion. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmQuotationConversionResult {
    private Long requestId;
    private Long engagementId;
    private Long contractId;
}
