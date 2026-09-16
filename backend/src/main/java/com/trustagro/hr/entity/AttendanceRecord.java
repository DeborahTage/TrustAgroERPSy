package com.trustagro.hr.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hr_attendance",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_hr_attendance_employee_date", columnNames = {"employee_id", "attendance_date"})
        },
        indexes = {
                @Index(name = "idx_hr_attendance_date", columnList = "attendance_date")
        })
@Data
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    private LocalTime checkIn;
    private LocalTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    /** Derived from checkIn/checkOut by the backend — never accepted from the client. */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal hoursWorked = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
