package com.trustagro.crm.entity;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_order_number", columnNames = "order_number"),
        @UniqueConstraint(name = "uk_customer_order_quotation", columnNames = "quotation_id")
})
@Data
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private CrmClient client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private CrmQuotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private CrmOpportunity opportunity;

    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(length = 8)
    private String currency = "ETB";

    @Convert(converter = CustomerOrderStatusConverter.class)
    @Column(length = 40)
    private CustomerOrderStatus status = CustomerOrderStatus.DRAFT;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private String deliveryAddress;
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
