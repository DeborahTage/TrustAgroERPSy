package com.trustagro.farm.entity;

import com.trustagro.veterinary.entity.MortalityCause;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "daily_mortality_lines")
@Data
public class DailyMortalityLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyFarmRecord dailyRecord;

    @Column(nullable = false)
    private Integer deathCount = 0;

    @Enumerated(EnumType.STRING)
    private MortalityCause cause;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
