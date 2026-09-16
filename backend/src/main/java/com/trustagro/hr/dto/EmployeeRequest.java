package com.trustagro.hr.dto;

import com.trustagro.hr.entity.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeRequest {
    /** Optional ERP login to link. Employee numbers are always backend-generated. */
    private Long userId;
    @NotBlank
    private String firstName;
    private String middleName;
    @NotBlank
    private String lastName;
    private String phone;
    @Email
    private String email;
    private String address;
    private LocalDate hireDate;
    private EmploymentType employmentType;
    private Long departmentId;
    private Long positionId;
    private Long managerId;
    @DecimalMin("0.0")
    private BigDecimal basicSalary;
    private String currency;
    private String bankName;
    private String bankAccountRef;
    private String emergencyContact;
    private String notes;
}
