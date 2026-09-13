package com.trustagro.pharmacy.entity;

import com.trustagro.user.entity.User;
import com.trustagro.veterinary.entity.Prescription;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pharmacy_prescriptions")
@Data
public class PharmacyPrescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String prescriptionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_prescription_id")
    private Prescription vetPrescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PharmacyPatient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescribed_by", nullable = false)
    private User prescribedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(length = 20)
    private String status = "Pending";

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Type(JsonType.class)
    @Column(columnDefinition = "text")
    private List<Object> durWarnings;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCost;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getPrescriptionCode() { return prescriptionCode; }
    public void setPrescriptionCode(String prescriptionCode) { this.prescriptionCode = prescriptionCode; }
    public PharmacyPatient getPatient() { return patient; }
    public void setPatient(PharmacyPatient patient) { this.patient = patient; }
    public List<Object> getDurWarnings() { return durWarnings; }
    public void setDurWarnings(java.util.List<Object> durWarnings) { this.durWarnings = durWarnings; }
    public void setStatus(String status) { this.status = status; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public Long getId() { return id; }
    public void setVetPrescription(Prescription vetPrescription) { this.vetPrescription = vetPrescription; }
    public void setPrescribedBy(User prescribedBy) { this.prescribedBy = prescribedBy; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
