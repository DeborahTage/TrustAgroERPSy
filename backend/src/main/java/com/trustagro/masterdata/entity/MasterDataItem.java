package com.trustagro.masterdata.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "master_data_items", uniqueConstraints = {
    @UniqueConstraint(name = "uq_master_data_category_value", columnNames = {"category", "item_value"})
})
@Data
public class MasterDataItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MasterDataCategory category;

    /** Internal code value (e.g. "BROILER", "LAYER") */
    @Column(name = "item_value", nullable = false, length = 100)
    private String value;

    /** Human-readable label (e.g. "Broiler Chicken") */
    @Column(nullable = false, length = 150)
    private String label;

    @Column(length = 500)
    private String description;

    /** Sort order for display */
    private Integer sortOrder;

    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void setCategory(MasterDataCategory category) { this.category = category; }
    public void setValue(String value) { this.value = value; }
    public void setLabel(String label) { this.label = label; }
    public void setDescription(String description) { this.description = description; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setActive(boolean active) { this.active = active; }

    public Long getId() { return id; }
    public MasterDataCategory getCategory() { return category; }
    public String getValue() { return value; }
    public String getLabel() { return label; }
    public boolean isActive() { return active; }
}
