package com.trustagro.farm.entity;

import com.trustagro.user.entity.User;
import com.trustagro.veterinary.entity.MortalityCause;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_farm_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_id", "flock_id", "date"})
})
@Data
public class DailyFarmRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    private Integer openingBirdCount;
    private Integer mortality;

    @Enumerated(EnumType.STRING)
    private MortalityCause mortalityCause;

    @Column(columnDefinition = "TEXT")
    private String mortalityNotes;

    private Integer culledChickens;
    private BigDecimal feedConsumed;
    private BigDecimal waterConsumed;
    private BigDecimal averageWeight;
    private String symptomsOrRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_case_id")
    private com.trustagro.veterinary.entity.DiseaseCase diseaseCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "silo_id")
    private com.trustagro.feed.entity.Silo silo;

    private String barnPhotoUrl;

    @OneToMany(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyFeedUsageLine> feedLines = new ArrayList<>();

    @OneToMany(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyMedicationLine> medicationLines = new ArrayList<>();

    @OneToMany(mappedBy = "dailyRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyMortalityLine> mortalityLines = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public Farm getFarm() { return farm; }
    public Flock getFlock() { return flock; }
    public Integer getOpeningBirdCount() { return openingBirdCount; }
    public Integer getMortality() { return mortality; }
    public MortalityCause getMortalityCause() { return mortalityCause; }
    public String getMortalityNotes() { return mortalityNotes; }
    public Integer getCulledChickens() { return culledChickens; }
    public BigDecimal getFeedConsumed() { return feedConsumed; }
    public BigDecimal getWaterConsumed() { return waterConsumed; }
    public BigDecimal getAverageWeight() { return averageWeight; }
    public String getSymptomsOrRemarks() { return symptomsOrRemarks; }
    public User getRecordedBy() { return recordedBy; }
    public com.trustagro.veterinary.entity.DiseaseCase getDiseaseCase() { return diseaseCase; }
    public com.trustagro.feed.entity.Silo getSilo() { return silo; }
    public String getBarnPhotoUrl() { return barnPhotoUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setDate(LocalDate date) { this.date = date; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public void setFlock(Flock flock) { this.flock = flock; }
    public void setOpeningBirdCount(Integer openingBirdCount) { this.openingBirdCount = openingBirdCount; }
    public void setMortality(Integer mortality) { this.mortality = mortality; }
    public void setMortalityCause(MortalityCause mortalityCause) { this.mortalityCause = mortalityCause; }
    public void setMortalityNotes(String mortalityNotes) { this.mortalityNotes = mortalityNotes; }
    public void setCulledChickens(Integer culledChickens) { this.culledChickens = culledChickens; }
    public void setFeedConsumed(BigDecimal feedConsumed) { this.feedConsumed = feedConsumed; }
    public void setWaterConsumed(BigDecimal waterConsumed) { this.waterConsumed = waterConsumed; }
    public void setAverageWeight(BigDecimal averageWeight) { this.averageWeight = averageWeight; }
    public void setSymptomsOrRemarks(String symptomsOrRemarks) { this.symptomsOrRemarks = symptomsOrRemarks; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }
    public void setDiseaseCase(com.trustagro.veterinary.entity.DiseaseCase diseaseCase) { this.diseaseCase = diseaseCase; }
    public void setSilo(com.trustagro.feed.entity.Silo silo) { this.silo = silo; }
    public void setBarnPhotoUrl(String barnPhotoUrl) { this.barnPhotoUrl = barnPhotoUrl; }

    public List<DailyFeedUsageLine> getFeedLines() { return feedLines; }
    public List<DailyMedicationLine> getMedicationLines() { return medicationLines; }
    public List<DailyMortalityLine> getMortalityLines() { return mortalityLines; }

    public void clearFeedLines() {
        feedLines.forEach(l -> l.setDailyRecord(null));
        feedLines.clear();
    }

    public void clearMedicationLines() {
        medicationLines.forEach(l -> l.setDailyRecord(null));
        medicationLines.clear();
    }

    public void clearMortalityLines() {
        mortalityLines.forEach(l -> l.setDailyRecord(null));
        mortalityLines.clear();
    }

    public void addFeedLine(DailyFeedUsageLine line) {
        feedLines.add(line);
        line.setDailyRecord(this);
    }

    public void addMedicationLine(DailyMedicationLine line) {
        medicationLines.add(line);
        line.setDailyRecord(this);
    }

    public void addMortalityLine(DailyMortalityLine line) {
        mortalityLines.add(line);
        line.setDailyRecord(this);
    }
}
