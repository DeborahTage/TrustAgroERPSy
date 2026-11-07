package com.trustagro.farm.dto;

import com.trustagro.user.entity.OrganizationalDesignation;
import lombok.Data;

@Data
public class EligibleSiteManagerResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private OrganizationalDesignation designation;
    private Long assignedFarmId;
    private String assignedFarmName;
    private boolean eligible;
}

