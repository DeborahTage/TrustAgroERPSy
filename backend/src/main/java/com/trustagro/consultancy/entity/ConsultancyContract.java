package com.trustagro.consultancy.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_contracts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_contract_number", columnNames = "contract_number"),
        @UniqueConstraint(name = "uk_consultancy_contract_engagement", columnNames = "engagement_id")
})
@Data
public class ConsultancyContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", nullable = false, length = 40)
    private String contractNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private ConsultancyEngagement engagement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String terms;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal contractValue = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.DRAFT;

    private LocalDate signedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
