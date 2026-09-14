package com.trustagro.training.dto;

import com.trustagro.training.entity.CertificateType;
import lombok.Data;

@Data
public class CertificateIssueRequest {
    private CertificateType certificateType;
}
