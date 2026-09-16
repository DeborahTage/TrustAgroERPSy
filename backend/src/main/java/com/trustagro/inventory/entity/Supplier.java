package com.trustagro.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Data
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique supplier code e.g. SUP-000001. Nullable for legacy rows; generated on create. */
    @Column(unique = true, length = 32)
    private String supplierCode;

    @Column(nullable = false)
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    @Column(columnDefinition = "TEXT")
    private String category; // e.g. FEED, DRUGS, EQUIPMENT

    /** CASH, NET_7, NET_15, NET_30, NET_60 */
    private String paymentTerms;

    private Integer rating; // 1-5

    private String taxIdentifier;

    @Column(length = 8)
    private String currency = "ETB";

    @Column(nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCategory() { return category; }
    public String getPaymentTerms() { return paymentTerms; }
    public Integer getRating() { return rating; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getTaxIdentifier() { return taxIdentifier; }
    public void setTaxIdentifier(String taxIdentifier) { this.taxIdentifier = taxIdentifier; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setCategory(String category) { this.category = category; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public void setRating(Integer rating) { this.rating = rating; }
}
