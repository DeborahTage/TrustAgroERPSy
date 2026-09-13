package com.trustagro.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(nullable = false)
    private String productName; // e.g., Eggs Grade A, Broiler Meat

    private String category; // EGGS, MEAT
    
    private Long flockId; // Source flock for biological traceability

    private Double quantity;

    private String unit; // TRAY, KG

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    public void setOrder(CustomerOrder order) { this.order = order; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Double getQuantity() { return quantity; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Long getFlockId() { return flockId; }
    public String getCategory() { return category; }
}
