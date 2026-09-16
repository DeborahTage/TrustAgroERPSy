package com.trustagro.hr.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hr_employee_number", columnNames = "employee_number"),
                @UniqueConstraint(name = "uk_hr_employee_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_hr_employee_status", columnList = "employment_status"),
                @Index(name = "idx_hr_employee_department", columnList = "department_id")
        })
@Data
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, length = 40)
    private String employeeNumber;

    /** Optional ERP login. An employee without a user simply has no system access. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @Column(nullable = false)
    private String lastName;

    private String phone;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private HrDepartment department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private HrPosition position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private EmployeeProfile manager;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    private String bankName;

    /** Reference only (e.g. masked account or internal payment reference) — never full credentials. */
    private String bankAccountRef;

    private String emergencyContact;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate terminationDate;

    @Column(columnDefinition = "TEXT")
    private String terminationReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName != null ? firstName : "");
        if (middleName != null && !middleName.isBlank()) {
            sb.append(' ').append(middleName);
        }
        if (lastName != null && !lastName.isBlank()) {
            sb.append(' ').append(lastName);
        }
        return sb.toString().trim();
    }
}
