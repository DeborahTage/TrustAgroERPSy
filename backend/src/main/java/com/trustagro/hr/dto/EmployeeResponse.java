package com.trustagro.hr.dto;

import com.trustagro.hr.entity.EmploymentStatus;
import com.trustagro.hr.entity.EmploymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResponse {
    private Long id;
    private String employeeNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private LocalDate hireDate;
    private EmploymentType employmentType;
    private EmploymentStatus employmentStatus;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private Long managerId;
    private String managerName;
    /** Null unless the caller is cleared to see compensation. */
    private BigDecimal basicSalary;
    private String currency;
    private String bankName;
    private String bankAccountRef;
    private String emergencyContact;
    private String notes;
    private LocalDate terminationDate;
    private String terminationReason;
    private Long userId;
    /** True when this employee has an ERP login attached. */
    private boolean erpAccessEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
