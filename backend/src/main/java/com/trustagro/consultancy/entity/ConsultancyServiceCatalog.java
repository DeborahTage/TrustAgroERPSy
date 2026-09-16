package com.trustagro.consultancy.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_services", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_service_code", columnNames = "service_code")
})
@Data
public class ConsultancyServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, length = 40)
    private String serviceCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category = ServiceCategory.OTHER;

    private Integer defaultDurationDays;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    private boolean taxApplicable = false;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
