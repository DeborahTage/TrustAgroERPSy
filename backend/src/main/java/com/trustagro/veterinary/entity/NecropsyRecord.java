package com.trustagro.veterinary.entity;

import com.trustagro.farm.entity.Flock;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "necropsy_records")
@Data
public class NecropsyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    private LocalDate necropsyDate = LocalDate.now();

    private Integer numChickensExamined;

    @Column(columnDefinition = "TEXT")
    private String heartFindings;

    @Column(columnDefinition = "TEXT")
    private String liverFindings;

    @Column(columnDefinition = "TEXT")
    private String spleenFindings;

    @Column(columnDefinition = "TEXT")
    private String lungFindings;

    @Column(columnDefinition = "TEXT")
    private String gizzardFindings;

    @Column(columnDefinition = "TEXT")
    private String intestineFindings;

    @Column(columnDefinition = "TEXT")
    private String generalFindings;

    private String suspectedCauseOfDeath;
    private String finalDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    // JSON or comma separated URLs for photos
    @Column(columnDefinition = "TEXT")
    private String photoUrls;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Accessors
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Flock getFlock() { return flock; }
    public void setFlock(Flock flock) { this.flock = flock; }
    public LocalDate getNecropsyDate() { return necropsyDate; }
    public void setNecropsyDate(LocalDate necropsyDate) { this.necropsyDate = necropsyDate; }
    public String getSuspectedCauseOfDeath() { return suspectedCauseOfDeath; }
    public void setSuspectedCauseOfDeath(String suspectedCauseOfDeath) { this.suspectedCauseOfDeath = suspectedCauseOfDeath; }
    public String getFinalDiagnosis() { return finalDiagnosis; }
    public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getPerformedBy() { return performedBy; }
    public void setPerformedBy(User u) { this.performedBy = u; }
    public void setNumChickensExamined(Integer n) { this.numChickensExamined = n; }
    public void setHeartFindings(String s) { this.heartFindings = s; }
    public void setLiverFindings(String s) { this.liverFindings = s; }
    public void setSpleenFindings(String s) { this.spleenFindings = s; }
    public void setLungFindings(String s) { this.lungFindings = s; }
    public void setGizzardFindings(String s) { this.gizzardFindings = s; }
    public void setIntestineFindings(String s) { this.intestineFindings = s; }
    public void setGeneralFindings(String s) { this.generalFindings = s; }
    public void setRecommendations(String s) { this.recommendations = s; }
    public void setPhotoUrls(String s) { this.photoUrls = s; }
}
