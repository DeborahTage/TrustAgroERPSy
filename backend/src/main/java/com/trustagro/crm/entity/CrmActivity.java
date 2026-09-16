package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_activities")
@Data
public class CrmActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrmActivityType type = CrmActivityType.NOTE;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

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
    @JoinColumn(name = "performed_by")
    private User performedBy;

    private LocalDateTime activityDate;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
