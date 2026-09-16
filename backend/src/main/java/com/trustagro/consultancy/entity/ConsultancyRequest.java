package com.trustagro.consultancy.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_request_number", columnNames = "request_number"),
        @UniqueConstraint(name = "uk_consultancy_request_crm_quotation", columnNames = "crm_quotation_id")
})
@Data
public class ConsultancyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, length = 40)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ConsultancyServiceCatalog requestedService;

    private LocalDate requestedDate;
    private String preferredLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestPriority priority = RequestPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_consultant_id")
    private User assignedConsultant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** CRM quotation that spawned this request (nullable unique for idempotency). */
    @Column(name = "crm_quotation_id")
    private Long crmQuotationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
