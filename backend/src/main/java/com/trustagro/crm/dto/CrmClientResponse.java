package com.trustagro.crm.dto;

import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CustomerType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrmClientResponse {
    private Long id;
    private String customerCode;
    private String clientName;
    private String phone;
    private String email;
    private String contactPerson;
    private String address;
    private String city;
    private String region;
    private String location;
    private String farmType;
    private String farmSize;
    private Integer numberOfBirds;
    private CustomerType customerType;
    private String taxIdentifier;
    private String notes;
    private ClientStatus status;
    private Long assignedExtensionWorkerId;
    private String assignedExtensionWorkerName;
    private Long assignedOwnerId;
    private String assignedOwnerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
