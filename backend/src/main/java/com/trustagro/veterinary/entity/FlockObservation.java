package com.trustagro.veterinary.entity;

import com.trustagro.farm.entity.Flock;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flock_observations")
@Data
public class FlockObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    private LocalDate observationDate = LocalDate.now();

    // 0 = Normal, 1 = Mild, 2 = Moderate, 3 = Severe
    private Integer respiratoryDistressScore = 0;
    
    // NONE, WATERY, BLOODY, YELLOW
    private String diarrheaType = "NONE";

    private Double feedIntakeDropPercentage = 0.0;
    private Double waterIntakeDropPercentage = 0.0;
    
    private Integer mortalityCount = 0;

    @Column(columnDefinition = "TEXT")
    private String generalComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observed_by", nullable = false)
    private User observedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Accessors
    public Long getId() { return id; }
    public void setObservedBy(User observedBy) { this.observedBy = observedBy; }
    public Integer getMortalityCount() { return mortalityCount; }
    public void setMortalityCount(Integer mortalityCount) { this.mortalityCount = mortalityCount; }
    public String getGeneralComments() { return generalComments; }
    public void setGeneralComments(String generalComments) { this.generalComments = generalComments; }
    public Double getFeedIntakeDropPercentage() { return feedIntakeDropPercentage; }
    public void setFeedIntakeDropPercentage(Double d) { this.feedIntakeDropPercentage = d; }
    public Double getWaterIntakeDropPercentage() { return waterIntakeDropPercentage; }
    public void setWaterIntakeDropPercentage(Double d) { this.waterIntakeDropPercentage = d; }
    public Integer getRespiratoryDistressScore() { return respiratoryDistressScore; }
    public void setRespiratoryDistressScore(Integer i) { this.respiratoryDistressScore = i; }
    public String getDiarrheaType() { return diarrheaType; }
    public void setDiarrheaType(String s) { this.diarrheaType = s; }
    public Flock getFlock() { return flock; }
    public void setFlock(Flock flock) { this.flock = flock; }
    public LocalDate getObservationDate() { return observationDate; }
    public void setObservationDate(LocalDate observationDate) { this.observationDate = observationDate; }
}
