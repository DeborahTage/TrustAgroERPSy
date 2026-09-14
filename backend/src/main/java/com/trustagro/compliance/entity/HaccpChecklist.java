package com.trustagro.compliance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "haccp_checklists")
@Data
public class HaccpChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String processName; // e.g., Poultry slaughter, feed handling

    private String ccpPoint; // Critical Control Point description

    private String requirement;

    private String monitoringFrequency;

    private String correctiveAction;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
