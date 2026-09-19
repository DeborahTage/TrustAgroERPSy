package com.trustagro.farm.sale.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.farm.entity.Barn;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
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
@Table(name = "chicken_sales")
@Data
public class ChickenSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String saleNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barn_id")
    private Barn barn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChickenSaleStatus status = ChickenSaleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChickenSaleType saleType = ChickenSaleType.FARM_GATE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Version
    private Long version;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChickenSaleItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addItem(ChickenSaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setSale(null));
        items.clear();
    }

    public Integer totalChickens() {
        return items.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
    }
}
