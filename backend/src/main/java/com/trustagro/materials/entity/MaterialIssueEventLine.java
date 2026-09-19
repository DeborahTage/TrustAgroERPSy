package com.trustagro.materials.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "material_issue_event_lines")
@Data
public class MaterialIssueEventLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_event_id", nullable = false)
    private MaterialIssueEvent issueEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_line_id")
    private MaterialRequestLine requestLine;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityIssued;

    private Long stockMovementId;
}
