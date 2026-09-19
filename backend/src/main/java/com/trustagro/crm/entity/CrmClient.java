package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_clients", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_client_customer_code", columnNames = "customer_code")
})
@Data
public class CrmClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", length = 40)
    private String customerCode;

    @Column(nullable = false)
    private String clientName;

    private String phone;
    private String email;
    private String contactPerson;
    private String address;
    private String city;
    private String region;
    private String location;
    private String farmType;
    private String farmSize;
    private Integer numberOfChickens;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CustomerType customerType;

    @Column(length = 80)
    private String taxIdentifier;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private ClientStatus status = ClientStatus.LEAD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_extension_worker_id")
    private User assignedExtensionWorker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_owner_id")
    private User assignedOwner;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
