package com.trustagro.consultancy.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "consultancy_activities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_activity_number", columnNames = "activity_number")
})
@Data
public class ConsultancyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_number", nullable = false, length = 40)
    private String activityNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private ConsultancyEngagement engagement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id")
    private User consultant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType = ActivityType.OTHER;

    @Column(nullable = false)
    private LocalDate activityDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status = ActivityStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
