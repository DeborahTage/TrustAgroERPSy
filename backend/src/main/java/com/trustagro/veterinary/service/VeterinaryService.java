package com.trustagro.veterinary.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.entity.DailyFarmRecord;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.dto.StockOutRequest;
import com.trustagro.inventory.entity.IssuedToType;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.veterinary.dto.*;
import com.trustagro.veterinary.entity.*;
import com.trustagro.veterinary.repository.*;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VeterinaryService {

    private static final String MODULE = "VETERINARY";

    private final VaccinationScheduleRepository vaccinationRepo;
    private final DiseaseCaseRepository diseaseCaseRepo;
    private final TreatmentRecordRepository treatmentRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final HealthIssueReportRepository healthIssueReportRepo;
    private final NecropsyRecordRepository necropsyRepo;
    private final FlockObservationRepository observationRepo;
    private final VaccinationTemplateRepository vaccinationTemplateRepo;
    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;
    private final DailyFarmRecordRepository dailyFarmRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final InventoryItemRepository inventoryItemRepository;
    private final FinanceService financeService;
    private final AuditService auditService;

    // EMR Aggregation
    public FlockEMRResponse getFlockEMR(Long flockId) {
        Flock flock = flockRepository.findById(flockId)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        
        FlockEMRResponse emr = new FlockEMRResponse();
        emr.setFlockId(flock.getId());
        emr.setBatchCode(flock.getBatchCode());
        emr.setBreed(flock.getBreed());
        
        long age = java.time.temporal.ChronoUnit.DAYS.between(flock.getStartDate(), LocalDate.now());
        emr.setAgeDays((int) age);
        
        emr.setDiseaseHistory(diseaseCaseRepo.findByFlock(flock).stream().map(this::toDiseaseCaseResponse).collect(Collectors.toList()));
        emr.setTreatments(treatmentRepo.findByFlock(flock).stream().map(this::toTreatmentResponse).collect(Collectors.toList()));
        emr.setVaccinations(vaccinationRepo.findByFlock(flock).stream().map(this::toVaccinationResponse).collect(Collectors.toList()));
        emr.setNecropsies(necropsyRepo.findByFlockId(flockId).stream().map(this::toNecropsyResponse).collect(Collectors.toList()));
        emr.setObservations(observationRepo.findByFlockId(flockId).stream().map(this::toObservationResponse).collect(Collectors.toList()));
        
        Integer totalMortality = dailyFarmRecordRepository.sumMortalityByFlock(flockId);
        emr.setTotalMortality(totalMortality != null ? totalMortality : 0);
        
        if (flock.getInitialBirdCount() != null && flock.getInitialBirdCount() > 0) {
            emr.setMortalityRate((emr.getTotalMortality() * 100.0) / flock.getInitialBirdCount());
        }
        
        emr.setIsUnderWithdrawal(flock.getIsUnderWithdrawal());
        emr.setWithdrawalEndDate(flock.getWithdrawalHoldUntil());
        
        return emr;
    }

    // Necropsy logic
    public NecropsyResponse createNecropsy(NecropsyRequest req) {
        NecropsyRecord nr = new NecropsyRecord();
        nr.setFlock(flockRepository.findById(req.getFlockId()).orElseThrow());
        nr.setNumBirdsExamined(req.getNumBirdsExamined());
        nr.setHeartFindings(req.getHeartFindings());
        nr.setLiverFindings(req.getLiverFindings());
        nr.setSpleenFindings(req.getSpleenFindings());
        nr.setLungFindings(req.getLungFindings());
        nr.setGizzardFindings(req.getGizzardFindings());
        nr.setIntestineFindings(req.getIntestineFindings());
        nr.setGeneralFindings(req.getGeneralFindings());
        nr.setSuspectedCauseOfDeath(req.getSuspectedCauseOfDeath());
        nr.setFinalDiagnosis(req.getFinalDiagnosis());
        nr.setRecommendations(req.getRecommendations());
        nr.setPhotoUrls(req.getPhotoUrls());
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(nr::setPerformedBy);
        
        NecropsyRecord saved = necropsyRepo.save(nr);
        return toNecropsyResponse(saved);
    }

    // Observation logic
    public FlockObservationResponse logObservation(FlockObservationRequest req) {
        FlockObservation obs = observationRepo.findByFlockIdAndObservationDate(req.getFlockId(), LocalDate.now())
                .orElse(new FlockObservation());
        
        obs.setFlock(flockRepository.findById(req.getFlockId()).orElseThrow());
        obs.setRespiratoryDistressScore(req.getRespiratoryDistressScore());
        obs.setDiarrheaType(req.getDiarrheaType());
        obs.setFeedIntakeDropPercentage(req.getFeedIntakeDropPercentage());
        obs.setWaterIntakeDropPercentage(req.getWaterIntakeDropPercentage());
        obs.setEggProductionDropPercentage(req.getEggProductionDropPercentage());
        obs.setMortalityCount(req.getMortalityCount());
        obs.setGeneralComments(req.getGeneralComments());
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(obs::setObservedBy);
        
        return toObservationResponse(observationRepo.save(obs));
    }

    // Withdrawal Enforcement
    @Transactional
    public void setFlockWithdrawal(Long flockId, Integer days) {
        Flock flock = flockRepository.findById(flockId).orElseThrow();
        flock.setIsUnderWithdrawal(true);
        flock.setWithdrawalHoldUntil(LocalDate.now().plusDays(days));
        flockRepository.save(flock);
        
        notificationService.createRoleNotification(
            "FLOCK WITHDRAWAL LOCK",
            "Flock " + flock.getBatchCode() + " is now under withdrawal lock until " + flock.getWithdrawalHoldUntil(),
            com.trustagro.user.entity.Role.INVENTORY_MANAGER
        );
    }

    private NecropsyResponse toNecropsyResponse(NecropsyRecord nr) {
        NecropsyResponse r = new NecropsyResponse();
        r.setId(nr.getId());
        r.setBatchCode(nr.getFlock().getBatchCode());
        r.setNecropsyDate(nr.getNecropsyDate());
        r.setSuspectedCauseOfDeath(nr.getSuspectedCauseOfDeath());
        r.setFinalDiagnosis(nr.getFinalDiagnosis());
        if (nr.getPerformedBy() != null) r.setPerformedBy(nr.getPerformedBy().getFullName());
        r.setCreatedAt(nr.getCreatedAt());
        return r;
    }

    private FlockObservationResponse toObservationResponse(FlockObservation obs) {
        FlockObservationResponse r = new FlockObservationResponse();
        r.setId(obs.getId());
        r.setObservationDate(obs.getObservationDate());
        r.setRespiratoryDistressScore(obs.getRespiratoryDistressScore());
        r.setDiarrheaType(obs.getDiarrheaType());
        r.setFeedIntakeDropPercentage(obs.getFeedIntakeDropPercentage());
        r.setMortalityCount(obs.getMortalityCount());
        r.setGeneralComments(obs.getGeneralComments());
        return r;
    }

    // Vaccinations
    public List<VaccinationResponse> getAllVaccinations() {
        return vaccinationRepo.findAll().stream().map(this::toVaccinationResponse).collect(Collectors.toList());
    }

    public VaccinationResponse createVaccination(VaccinationRequest req) {
        VaccinationSchedule v = new VaccinationSchedule();
        v.setFarm(farmRepository.findById(req.getFarmId()).orElseThrow(() -> new ResourceNotFoundException("Farm not found")));
        v.setFlock(flockRepository.findById(req.getFlockId()).orElseThrow(() -> new ResourceNotFoundException("Flock not found")));
        v.setVaccineName(req.getVaccineName());
        v.setDiseaseProtectedAgainst(req.getDiseaseProtectedAgainst());
        v.setScheduledDate(req.getScheduledDate());
        v.setDosage(req.getDosage());
        v.setRoute(req.getRoute());
        v.setResponsiblePerson(req.getResponsiblePerson());
        v.setRemarks(req.getRemarks());
        VaccinationSchedule saved = vaccinationRepo.save(v);
        auditService.logObject("CREATE", MODULE, "VACCINATION", saved.getId(), null, toVaccinationResponse(saved));
        return toVaccinationResponse(saved);
    }

    public VaccinationResponse completeVaccination(Long id) {
        VaccinationSchedule v = vaccinationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found"));
        VaccinationResponse before = toVaccinationResponse(v);
        v.setStatus(VaccinationStatus.COMPLETED);
        v.setActualDate(LocalDate.now());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(v::setGivenBy);
        VaccinationSchedule saved = vaccinationRepo.save(v);
        auditService.logObject("COMPLETE", MODULE, "VACCINATION", saved.getId(), before, toVaccinationResponse(saved));
        return toVaccinationResponse(saved);
    }

    // Disease Cases
    public List<DiseaseCaseResponse> getAllDiseaseCases() {
        return diseaseCaseRepo.findAll().stream().map(this::toDiseaseCaseResponse).collect(Collectors.toList());
    }

    public List<DiseaseCaseResponse> getActiveDiseaseCases() {
        return diseaseCaseRepo.findByStatusOrderByDateDetectedDesc(DiseaseStatus.ACTIVE)
                .stream().map(this::toDiseaseCaseResponse).collect(Collectors.toList());
    }

    public DiseaseCaseResponse createDiseaseCase(DiseaseCaseRequest req) {
        DiseaseCase dc = new DiseaseCase();
        dc.setFarm(farmRepository.findById(req.getFarmId()).orElseThrow(() -> new ResourceNotFoundException("Farm not found")));
        dc.setFlock(flockRepository.findById(req.getFlockId()).orElseThrow(() -> new ResourceNotFoundException("Flock not found")));
        dc.setDateDetected(req.getDateDetected() != null ? req.getDateDetected() : LocalDate.now());
        dc.setSymptoms(req.getSymptoms());
        dc.setSuspectedDisease(req.getSuspectedDisease());
        dc.setNumberAffected(req.getNumberAffected());
        dc.setNumberDead(req.getNumberDead());
        dc.setSeverity(req.getSeverity());
        dc.setActionTaken(req.getActionTaken());
        if (req.getAttachmentUrl() != null) {
            dc.setAttachmentPath(req.getAttachmentUrl());
        }
        if (req.getStatus() != null) {
            dc.setStatus(req.getStatus());
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(dc::setReportedBy);
        DiseaseCase saved = diseaseCaseRepo.save(dc);
        auditService.logObject("CREATE", MODULE, "DISEASE_CASE", saved.getId(), null, toDiseaseCaseResponse(saved));
        return toDiseaseCaseResponse(saved);
    }

    public DiseaseCaseResponse updateDiseaseCase(Long id, DiseaseCaseRequest req) {
        DiseaseCase dc = diseaseCaseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disease case not found"));
        DiseaseCaseResponse before = toDiseaseCaseResponse(dc);
        dc.setSymptoms(req.getSymptoms());
        dc.setSuspectedDisease(req.getSuspectedDisease());
        dc.setNumberAffected(req.getNumberAffected());
        dc.setNumberDead(req.getNumberDead());
        dc.setSeverity(req.getSeverity());
        dc.setActionTaken(req.getActionTaken());
        if (req.getAttachmentUrl() != null) {
            dc.setAttachmentPath(req.getAttachmentUrl());
        }
        if (req.getStatus() != null) {
            dc.setStatus(req.getStatus());
        }
        DiseaseCase saved = diseaseCaseRepo.save(dc);
        auditService.logObject("UPDATE", MODULE, "DISEASE_CASE", saved.getId(), before, toDiseaseCaseResponse(saved));
        return toDiseaseCaseResponse(saved);
    }

    // Health Issue Reports
    public List<HealthIssueReportResponse> getAllHealthIssueReports() {
        Sort sort = Sort.by(
                Sort.Order.desc("reportDate"),
                Sort.Order.desc("createdAt")
        );
        return healthIssueReportRepo.findAll(sort)
                .stream().map(this::toHealthIssueReportResponse).collect(Collectors.toList());
    }

    public HealthIssueReportResponse createHealthIssueReport(HealthIssueReportRequest req) {
        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = flockRepository.findById(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));

        HealthIssueReport report = new HealthIssueReport();
        report.setFarm(farm);
        report.setFlock(flock);
        report.setSymptoms(req.getSymptoms());
        report.setMortalityObserved(req.getMortalityObserved());
        report.setNumberAffected(req.getNumberAffected());
        report.setRemarks(req.getRemarks());
        report.setReportDate(req.getReportDate() != null ? req.getReportDate() : LocalDate.now());
        if (req.getDailyFarmRecordId() != null) {
            DailyFarmRecord dailyRecord = dailyFarmRecordRepository.findById(req.getDailyFarmRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Daily farm record not found"));
            report.setDailyFarmRecord(dailyRecord);
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(report::setReportedBy);

        HealthIssueReport saved = healthIssueReportRepo.save(report);
        auditService.logObject("CREATE", MODULE, "HEALTH_REPORT", saved.getId(), null, toHealthIssueReportResponse(saved));
        notificationService.createRoleNotification(
                "New Health Issue Report",
                farm.getFarmName() + " / " + flock.getBatchCode() + " requires veterinary review.",
                com.trustagro.user.entity.Role.VETERINARIAN
        );
        return toHealthIssueReportResponse(saved);
    }

    public HealthIssueReportResponse reviewHealthIssueReport(Long id, HealthIssueReportReviewRequest req) {
        HealthIssueReport report = healthIssueReportRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Health issue report not found"));
        HealthIssueReportResponse before = toHealthIssueReportResponse(report);
        
        report.setSuspectedDiagnosis(req.getSuspectedDiagnosis());
        report.setSeverity(req.getSeverity());
        report.setTreatmentPlan(req.getTreatmentPlan());
        report.setDiseaseCaseId(req.getDiseaseCaseId());
        report.setReviewDate(LocalDate.now());
        if (req.getStatus() != null) {
            report.setStatus(req.getStatus());
        } else if (report.getStatus() == HealthIssueReportStatus.OPEN) {
            report.setStatus(HealthIssueReportStatus.REVIEWED);
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(report::setReviewedBy);
        HealthIssueReport saved = healthIssueReportRepo.save(report);
        auditService.logObject("REVIEW", MODULE, "HEALTH_REPORT", saved.getId(), before, toHealthIssueReportResponse(saved));
        return toHealthIssueReportResponse(saved);
    }

    // Treatments
    public List<TreatmentResponse> getAllTreatments() {
        return treatmentRepo.findAll().stream().map(this::toTreatmentResponse).collect(Collectors.toList());
    }

    @Transactional
    public TreatmentResponse createTreatment(TreatmentRequest req) {
        TreatmentRecord t = new TreatmentRecord();
        if (req.getDiseaseCaseId() != null) {
            t.setDiseaseCase(diseaseCaseRepo.findById(req.getDiseaseCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Disease case not found")));
        }
        t.setFarm(farmRepository.findById(req.getFarmId()).orElseThrow(() -> new ResourceNotFoundException("Farm not found")));
        t.setFlock(flockRepository.findById(req.getFlockId()).orElseThrow(() -> new ResourceNotFoundException("Flock not found")));
        t.setInventoryItemId(req.getInventoryItemId());
        t.setQuantity(req.getQuantity());
        t.setDrugName(req.getDrugName());
        t.setDosage(req.getDosage());
        t.setRoute(req.getRoute());
        t.setDuration(req.getDuration());
        t.setStartDate(req.getStartDate() != null ? req.getStartDate() : LocalDate.now());
        t.setEndDate(req.getEndDate());
        t.setOutcome(req.getOutcome());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(t::setVetOfficer);
        TreatmentRecord saved = treatmentRepo.save(t);
        deductStockIfApplicable(
                req.getInventoryItemId(),
                req.getQuantity(),
                req.getFarmId(),
                "TREATMENT",
                saved.getId(),
                "Treatment - " + req.getDrugName()
        );
        auditService.logObject("CREATE", MODULE, "TREATMENT", saved.getId(), null, toTreatmentResponse(saved));
        return toTreatmentResponse(saved);
    }

    public List<TreatmentResponse> getRecentTreatments(int count) {
        return treatmentRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().limit(count).map(this::toTreatmentResponse).collect(Collectors.toList());
    }

    // Prescriptions
    public List<PrescriptionResponse> getAllPrescriptions() {
        return prescriptionRepo.findAll().stream().map(this::toPrescriptionResponse).collect(Collectors.toList());
    }

    public PrescriptionResponse createPrescription(PrescriptionRequest req) {
        if (prescriptionRepo.existsByPrescriptionNumber(req.getPrescriptionNumber()))
            throw new BusinessException("Prescription number already exists");
        Prescription p = new Prescription();
        p.setPrescriptionNumber(req.getPrescriptionNumber());
        p.setPrescriptionType(req.getPrescriptionType() != null ? req.getPrescriptionType() : PrescriptionType.INTERNAL_FARM);
        p.setFarmId(req.getFarmId());
        p.setFlockId(req.getFlockId());
        p.setClientId(req.getClientId());
        p.setDiseaseCaseId(req.getDiseaseCaseId());
        p.setInventoryItemId(req.getInventoryItemId());
        p.setDrugName(req.getDrugName());
        p.setQuantity(req.getQuantity());
        p.setDosageInstruction(req.getDosageInstruction());
        p.setWithdrawalPeriodDays(req.getWithdrawalPeriodDays());
        
        if (req.getWithdrawalPeriodDays() != null && req.getWithdrawalPeriodDays() > 0) {
            p.setWithdrawalEndDate(LocalDate.now().plusDays(req.getWithdrawalPeriodDays()));
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(p::setCreatedByVet);
        p.setStatus(PrescriptionStatus.PENDING_PHARMACY);
        Prescription saved = prescriptionRepo.save(p);
        auditService.logObject("CREATE", MODULE, "PRESCRIPTION", saved.getId(), null, toPrescriptionResponse(saved));
        notificationService.createRoleNotification(
                "Prescription Ready for Dispensing",
                req.getPrescriptionNumber() + " has been sent to pharmacy.",
                com.trustagro.user.entity.Role.PHARMACIST
        );
        return toPrescriptionResponse(saved);
    }

    @Transactional
    public PrescriptionResponse approvePrescription(Long id) {
        Prescription p = prescriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        if (p.getStatus() == PrescriptionStatus.DISPENSED || p.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new BusinessException("Prescription already dispensed");
        }
        if (p.getInventoryItemId() != null) {
            InventoryItem item = inventoryItemRepository.findById(p.getInventoryItemId()).orElse(null);
            if (item != null && item.getCurrentStock() != null && p.getQuantity() != null
                    && item.getCurrentStock() < p.getQuantity()) {
                throw new BusinessException("Insufficient stock for " + item.getItemName());
            }
        }
        p.setStatus(PrescriptionStatus.APPROVED);
        Prescription saved = prescriptionRepo.save(p);
        notificationService.createRoleNotification(
                "Prescription Approved",
                "Order " + p.getPrescriptionNumber() + " approved by pharmacy.",
                com.trustagro.user.entity.Role.VETERINARIAN);
        auditService.logObject("APPROVE", MODULE, "PRESCRIPTION", saved.getId(), null, toPrescriptionResponse(saved));
        return toPrescriptionResponse(saved);
    }

    @Transactional
    public PrescriptionResponse dispensePrescription(Long id) {
        Prescription p = prescriptionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        if (p.getStatus() == PrescriptionStatus.CANCELLED)
            throw new BusinessException("Prescription is cancelled");
        if (p.getStatus() == PrescriptionStatus.DISPENSED)
            throw new BusinessException("Prescription already dispensed");
        if (p.getStatus() != PrescriptionStatus.APPROVED && p.getStatus() != PrescriptionStatus.PENDING_PHARMACY
                && p.getStatus() != PrescriptionStatus.PENDING) {
            throw new BusinessException("Prescription must be approved before dispensing");
        }
        String before = summarizePrescription(p);
        deductStockIfApplicable(
                p.getInventoryItemId(),
                p.getQuantity(),
                p.getFarmId(),
                "PRESCRIPTION",
                p.getId(),
                "Prescription Dispense - " + p.getPrescriptionNumber()
        );
        BigDecimal expenseAmount = BigDecimal.ZERO;
        if (p.getInventoryItemId() != null && p.getQuantity() != null) {
            InventoryItem item = inventoryItemRepository.findById(p.getInventoryItemId()).orElse(null);
            if (item != null) {
                BigDecimal unitCost = item.getAvgUnitCost() != null ? item.getAvgUnitCost()
                        : (item.getRetailPrice() != null ? item.getRetailPrice() : BigDecimal.ZERO);
                expenseAmount = unitCost.multiply(BigDecimal.valueOf(p.getQuantity()));
            }
        }
        if (expenseAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postExpense(expenseAmount, com.trustagro.finance.entity.BusinessUnit.PHARMACY, "5120",
                    "Vet dispense: " + p.getPrescriptionNumber() + " - " + p.getDrugName(),
                    "VETERINARY", "PRESCRIPTION", p.getId());
        }
        p.setStatus(PrescriptionStatus.DISPENSED);
        Prescription saved = prescriptionRepo.save(p);
        notificationService.createRoleNotification(
                "Prescription Dispensed",
                "Order " + p.getPrescriptionNumber() + " dispensed. Stock deducted and expense recorded.",
                com.trustagro.user.entity.Role.VETERINARIAN);
        notificationService.createRoleNotification(
                "Veterinary Expense Recorded",
                String.format("ETB %s posted for Rx %s", expenseAmount, p.getPrescriptionNumber()),
                com.trustagro.user.entity.Role.FINANCE);
        auditService.logObject("DISPENSE", MODULE, "PRESCRIPTION", saved.getId(), before, toPrescriptionResponse(saved));
        return toPrescriptionResponse(saved);
    }

    private void deductStockIfApplicable(Long inventoryItemId, Double quantity, Long farmId,
                                         String referenceType, Long referenceId, String reason) {
        if (inventoryItemId == null || quantity == null || quantity <= 0) {
            return;
        }
        StockOutRequest stockOut = new StockOutRequest();
        stockOut.setItemId(inventoryItemId);
        stockOut.setQuantity(quantity);
        stockOut.setReason(reason);
        stockOut.setIssuedToType(IssuedToType.FARM);
        stockOut.setFarmId(farmId);
        stockOut.setReferenceType(referenceType);
        stockOut.setReferenceId(referenceId);
        inventoryService.stockOut(stockOut);
    }

    private String summarizeVaccination(VaccinationSchedule v) {
        return v.getVaccineName() + " | " + v.getStatus() + " | scheduled=" + v.getScheduledDate();
    }

    private String summarizeDiseaseCase(DiseaseCase dc) {
        return dc.getSuspectedDisease() + " | " + dc.getStatus() + " | affected=" + dc.getNumberAffected();
    }

    private String summarizeTreatment(TreatmentRecord t) {
        return t.getDrugName() + " | qty=" + t.getQuantity() + " | outcome=" + t.getOutcome();
    }

    private String summarizePrescription(Prescription p) {
        return p.getPrescriptionNumber() + " | " + p.getDrugName() + " | qty=" + p.getQuantity() + " | " + p.getStatus();
    }

    public VaccinationResponse toVaccinationResponse(VaccinationSchedule v) {
        VaccinationResponse r = new VaccinationResponse();
        r.setId(v.getId());
        if (v.getFarm() != null) { r.setFarmId(v.getFarm().getId()); r.setFarmName(v.getFarm().getFarmName()); }
        if (v.getFlock() != null) { r.setFlockId(v.getFlock().getId()); r.setBatchCode(v.getFlock().getBatchCode()); }
        r.setVaccineName(v.getVaccineName());
        r.setDiseaseProtectedAgainst(v.getDiseaseProtectedAgainst());
        r.setScheduledDate(v.getScheduledDate());
        r.setActualDate(v.getActualDate());
        r.setDosage(v.getDosage());
        r.setRoute(v.getRoute());
        r.setResponsiblePerson(v.getResponsiblePerson());
        r.setStatus(v.getStatus());
        if (v.getGivenBy() != null) r.setGivenBy(v.getGivenBy().getFullName());
        r.setRemarks(v.getRemarks());
        r.setCreatedAt(v.getCreatedAt());
        return r;
    }

    public DiseaseCaseResponse toDiseaseCaseResponse(DiseaseCase dc) {
        DiseaseCaseResponse r = new DiseaseCaseResponse();
        r.setId(dc.getId());
        if (dc.getFarm() != null) { r.setFarmId(dc.getFarm().getId()); r.setFarmName(dc.getFarm().getFarmName()); }
        if (dc.getFlock() != null) { r.setFlockId(dc.getFlock().getId()); r.setBatchCode(dc.getFlock().getBatchCode()); }
        r.setDateDetected(dc.getDateDetected());
        r.setSymptoms(dc.getSymptoms());
        r.setSuspectedDisease(dc.getSuspectedDisease());
        r.setNumberAffected(dc.getNumberAffected());
        if (dc.getFlock() != null && dc.getDateDetected() != null) {
            LocalDate end = dc.getDateResolved() != null ? dc.getDateResolved() : LocalDate.now();
            Integer deaths = dailyFarmRecordRepository.sumMortalityBetweenAndFlock(dc.getDateDetected(), end, dc.getFlock().getId());
            r.setNumberDead(deaths != null && deaths > 0 ? deaths : dc.getNumberDead());
        } else {
            r.setNumberDead(dc.getNumberDead());
        }
        
        r.setSeverity(dc.getSeverity());
        r.setStatus(dc.getStatus());
        r.setActionTaken(dc.getActionTaken());
        r.setAttachmentUrl(dc.getAttachmentPath());
        if (dc.getReportedBy() != null) r.setReportedBy(dc.getReportedBy().getFullName());
        r.setCreatedAt(dc.getCreatedAt());
        return r;
    }

    public TreatmentResponse toTreatmentResponse(TreatmentRecord t) {
        TreatmentResponse r = new TreatmentResponse();
        r.setId(t.getId());
        if (t.getDiseaseCase() != null) r.setDiseaseCaseId(t.getDiseaseCase().getId());
        if (t.getFarm() != null) { r.setFarmId(t.getFarm().getId()); r.setFarmName(t.getFarm().getFarmName()); }
        if (t.getFlock() != null) { r.setFlockId(t.getFlock().getId()); r.setBatchCode(t.getFlock().getBatchCode()); }
        r.setInventoryItemId(t.getInventoryItemId());
        r.setQuantity(t.getQuantity());
        r.setDrugName(t.getDrugName());
        r.setDosage(t.getDosage());
        r.setRoute(t.getRoute());
        r.setDuration(t.getDuration());
        r.setStartDate(t.getStartDate());
        r.setEndDate(t.getEndDate());
        if (t.getVetOfficer() != null) {
            r.setVetId(t.getVetOfficer().getId());
            r.setVetOfficer(t.getVetOfficer().getFullName());
        }
        r.setOutcome(t.getOutcome());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }

    private PrescriptionResponse toPrescriptionResponse(Prescription p) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.setId(p.getId());
        r.setPrescriptionNumber(p.getPrescriptionNumber());
        r.setPrescriptionType(p.getPrescriptionType());
        r.setFarmId(p.getFarmId());
        if (p.getFarmId() != null) {
            farmRepository.findById(p.getFarmId()).ifPresent(f -> r.setFarmName(f.getFarmName()));
        }
        r.setFlockId(p.getFlockId());
        if (p.getFlockId() != null) {
            flockRepository.findById(p.getFlockId()).ifPresent(f -> r.setBatchCode(f.getBatchCode()));
        }
        r.setClientId(p.getClientId());
        r.setDiseaseCaseId(p.getDiseaseCaseId());
        r.setInventoryItemId(p.getInventoryItemId());
        r.setDrugName(p.getDrugName());
        r.setQuantity(p.getQuantity());
        r.setDosageInstruction(p.getDosageInstruction());
        r.setWithdrawalPeriodDays(p.getWithdrawalPeriodDays());
        r.setWithdrawalEndDate(p.getWithdrawalEndDate());
        if (p.getCreatedByVet() != null) {
            r.setPrescribedById(p.getCreatedByVet().getId());
            r.setCreatedByVet(p.getCreatedByVet().getFullName());
        }
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    private HealthIssueReportResponse toHealthIssueReportResponse(HealthIssueReport report) {
        HealthIssueReportResponse response = new HealthIssueReportResponse();
        response.setId(report.getId());
        if (report.getFarm() != null) {
            response.setFarmId(report.getFarm().getId());
            response.setFarmName(report.getFarm().getFarmName());
        }
        if (report.getFlock() != null) {
            response.setFlockId(report.getFlock().getId());
            response.setBatchCode(report.getFlock().getBatchCode());
        }
        if (report.getDailyFarmRecord() != null) {
            response.setDailyFarmRecordId(report.getDailyFarmRecord().getId());
        }
        response.setSymptoms(report.getSymptoms());
        response.setMortalityObserved(report.getMortalityObserved());
        response.setNumberAffected(report.getNumberAffected());
        response.setRemarks(report.getRemarks());
        if (report.getReportedBy() != null) response.setReportedBy(report.getReportedBy().getFullName());
        response.setReportDate(report.getReportDate());
        response.setStatus(report.getStatus());
        response.setSuspectedDiagnosis(report.getSuspectedDiagnosis());
        response.setSeverity(report.getSeverity());
        response.setTreatmentPlan(report.getTreatmentPlan());
        if (report.getReviewedBy() != null) response.setReviewedBy(report.getReviewedBy().getFullName());
        response.setReviewDate(report.getReviewDate());
        response.setDiseaseCaseId(report.getDiseaseCaseId());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        return response;
    }
}
