package com.trustagro.farm.entity;

import com.trustagro.inventory.entity.InventoryItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "daily_medication_lines")
@Data
public class DailyMedicationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyFarmRecord dailyRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    /** MEDICATION, VITAMIN, OTHER */
    @Column(length = 40, nullable = false)
    private String treatmentType = "MEDICATION";

    @Column(precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Long stockMovementId;

    /** When true, inventory was already posted by Veterinary/Pharmacy — do not stock-out again. */
    private boolean inventoryAlreadyPosted = false;
}
