package com.trustagro.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_entity", columnList = "entityType"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created", columnList = "createdAt"),
    @Index(name = "idx_audit_module", columnList = "module")
})
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String userEmail;

    /** CREATE, UPDATE, DELETE, VIEW, LOGIN_SUCCESS, etc. */
    @Column(nullable = false)
    private String action;

    /** High-level module name, e.g. INVENTORY, VETERINARY, SECURITY */
    @Column(nullable = false)
    private String module;

    /** Fine-grained entity type, e.g. VACCINATION, DISEASE_CASE, FLOCK */
    private String entityType;

    /** Primary key of the affected record */
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    /** SUCCESS or FAILURE — defaults to SUCCESS for existing rows */
    @Column(length = 16)
    private String result = "SUCCESS";

    @Column(length = 64)
    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
