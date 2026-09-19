package com.trustagro.farm.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_templates")
@Data
public class DocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private boolean active = false;

    @Column(length = 40)
    private String module;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
