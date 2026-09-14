package com.trustagro.farm.sale.entity;

import com.trustagro.farm.entity.Flock;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flock_population_events")
@Data
public class FlockPopulationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlockPopulationEventType eventType;

    @Column(nullable = false)
    private Integer previousPopulation;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private Integer newPopulation;

    private Long chickenSaleId;

    @Column(nullable = false)
    private LocalDate eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
