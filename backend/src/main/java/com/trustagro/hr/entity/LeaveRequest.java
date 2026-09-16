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
@Table(name = "hr_leave_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hr_leave_request_number", columnNames = "request_number")
        },
        indexes = {
                @Index(name = "idx_hr_leave_request_status", columnList = "status"),
                @Index(name = "idx_hr_leave_request_employee", columnList = "employee_id")
        })
@Data
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, length = 40)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** Calendar days inclusive, computed by the backend from the date range. */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal totalDays = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
