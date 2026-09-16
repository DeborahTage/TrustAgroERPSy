package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_tasks")
@Data
public class CrmTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private CrmClient client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private CrmLead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private CrmOpportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrmPriority priority = CrmPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrmTaskStatus status = CrmTaskStatus.TODO;

    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
