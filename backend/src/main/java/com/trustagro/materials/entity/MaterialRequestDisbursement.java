package com.trustagro.materials.entity;

import com.trustagro.inventory.entity.Supplier;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "material_request_disbursements",
        uniqueConstraints = @UniqueConstraint(name = "uk_mr_disbursement_number", columnNames = "disbursement_number"))
@Data
public class MaterialRequestDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_request_id", nullable = false)
    private MaterialRequest materialRequest;

    @Column(name = "disbursement_number", nullable = false, unique = true, length = 40)
    private String disbursementNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 40)
    private String paymentMethod;

    private LocalDate paymentDate;

    @Column(length = 500)
    private String evidenceUrl;

    private Long financeTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
