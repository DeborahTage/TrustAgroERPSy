package com.trustagro.materials.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "material_issue_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_mr_issue_key", columnNames = "idempotency_key"))
@Data
public class MaterialIssueEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @Column(nullable = false, unique = true, length = 40)
    private String issueNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_request_id", nullable = false)
    private MaterialRequest materialRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private boolean posted = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "issueEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MaterialIssueEventLine> lines = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void addLine(MaterialIssueEventLine line) {
        lines.add(line);
        line.setIssueEvent(this);
    }
}
