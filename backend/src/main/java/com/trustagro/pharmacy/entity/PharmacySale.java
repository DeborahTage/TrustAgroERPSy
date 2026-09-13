package com.trustagro.pharmacy.entity;

import com.trustagro.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pharmacy_sales")
@Data
public class PharmacySale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String saleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private PharmacyPrescription prescription;

    // Legacy internal patient link (kept for vet workflow)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PharmacyPatient patient;

    // New external commercial customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private PharmacyCustomer customer;

    // New proper line items (replaces JSON blob going forward)
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PharmacySaleItem> saleItems = new ArrayList<>();

    // Legacy JSON (kept read-only for existing data)
    @Type(JsonType.class)
    @Column(columnDefinition = "text")
    private List<Object> items;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 30)
    private String paymentMethod; // CASH, MOBILE_MONEY, BANK_TRANSFER, CREDIT

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Enumerated(EnumType.STRING)
    private PriceType priceType = PriceType.RETAIL;

    // Fallback for walk-in customers
    private String customerName;
    private String customerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by", nullable = false)
    private User soldBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Accessors
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSaleCode() { return saleCode; }
    public void setSaleCode(String saleCode) { this.saleCode = saleCode; }
    public PharmacyPrescription getPrescription() { return prescription; }
    public void setPrescription(PharmacyPrescription prescription) { this.prescription = prescription; }
    public PharmacyPatient getPatient() { return patient; }
    public void setPatient(PharmacyPatient patient) { this.patient = patient; }
    public PharmacyCustomer getCustomer() { return customer; }
    public void setCustomer(PharmacyCustomer customer) { this.customer = customer; }
    public List<PharmacySaleItem> getSaleItems() { return saleItems; }
    public void setSaleItems(List<PharmacySaleItem> saleItems) { this.saleItems = saleItems; }
    public List<Object> getItems() { return items; }
    public void setItems(List<Object> items) { this.items = items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public PriceType getPriceType() { return priceType; }
    public void setPriceType(PriceType priceType) { this.priceType = priceType; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public User getSoldBy() { return soldBy; }
    public void setSoldBy(User soldBy) { this.soldBy = soldBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
