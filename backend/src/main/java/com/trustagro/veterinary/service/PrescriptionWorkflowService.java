package com.trustagro.veterinary.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FarmCostAllocation;
import com.trustagro.finance.entity.FarmCostType;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.repository.FarmCostAllocationRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.dto.StockOutRequest;
import com.trustagro.inventory.entity.IssuedToType;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.dto.DispensePrescriptionRequest;
import com.trustagro.veterinary.dto.PrescriptionRequest;
import com.trustagro.veterinary.dto.PrescriptionResponse;
import com.trustagro.veterinary.dto.RejectPrescriptionRequest;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionPriority;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.entity.PrescriptionType;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single authoritative prescription workflow:
 * Vet create/submit → Pharmacy approve/reject/dispense → Inventory + Finance + Notify + Audit.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionWorkflowService {

    public static final String MODULE = "PRESCRIPTION";
    public static final String INVENTORY_REF = "VETERINARY_PRESCRIPTION";

    private static final Set<PrescriptionStatus> QUEUE_STATUSES = EnumSet.of(
            PrescriptionStatus.PENDING,
            PrescriptionStatus.SUBMITTED,
            PrescriptionStatus.PENDING_PHARMACY,
            PrescriptionStatus.APPROVED
    );

    private static final Set<PrescriptionStatus> DISPENSABLE = EnumSet.of(PrescriptionStatus.APPROVED);
    private static final Set<PrescriptionStatus> APPROVABLE = EnumSet.of(
            PrescriptionStatus.PENDING_PHARMACY, PrescriptionStatus.PENDING, PrescriptionStatus.SUBMITTED
    );
    private static final Set<PrescriptionStatus> CANCELABLE = EnumSet.of(
            PrescriptionStatus.DRAFT, PrescriptionStatus.SUBMITTED,
            PrescriptionStatus.PENDING, PrescriptionStatus.PENDING_PHARMACY
    );

    private final PrescriptionRepository prescriptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final FarmCostAllocationRepository farmCostAllocationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;

    public List<PrescriptionResponse> listAll() {
        return prescriptionRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PrescriptionResponse> pharmacyQueue(String status, String priority, Long farmId, Long flockId, Long vetId) {
        return prescriptionRepository.findAll().stream()
                .filter(p -> status != null && !status.isBlank()
                        ? p.getStatus().name().equalsIgnoreCase(status)
                        : QUEUE_STATUSES.contains(p.getStatus()))
                .filter(p -> priority == null || priority.isBlank()
                        || (p.getPriority() != null && p.getPriority().name().equalsIgnoreCase(priority)))
                .filter(p -> farmId == null || farmId.equals(p.getFarmId()))
                .filter(p -> flockId == null || flockId.equals(p.getFlockId()))
                .filter(p -> vetId == null || (p.getCreatedByVet() != null && vetId.equals(p.getCreatedByVet().getId())))
                .sorted((a, b) -> {
                    int pri = priorityRank(b.getPriority()) - priorityRank(a.getPriority());
                    if (pri != 0) return pri;
                    LocalDateTime ca = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime cb = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return cb.compareTo(ca);
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PrescriptionResponse getById(Long id) {
        return toResponse(require(id), true);
    }

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest req) {
        validateCreate(req);
        if (prescriptionRepository.existsByPrescriptionNumber(req.getPrescriptionNumber())) {
            throw new BusinessException("Prescription number already exists");
        }
        User vet = currentUser();
        Prescription p = new Prescription();
        applyRequest(p, req);
        p.setCreatedByVet(vet);
        p.setPrescribedDate(LocalDate.now());
        boolean submitNow = req.getSubmitImmediately() == null || Boolean.TRUE.equals(req.getSubmitImmediately());
        p.setStatus(submitNow ? PrescriptionStatus.PENDING_PHARMACY : PrescriptionStatus.DRAFT);
        applyWithdrawal(p, req.getWithdrawalPeriodDays(), LocalDate.now());
        Prescription saved = prescriptionRepository.save(p);
        auditService.logObject("CREATE", MODULE, "PRESCRIPTION", saved.getId(), null, toResponse(saved));
        if (submitNow) {
            notifyPharmacists(saved);
            auditService.logObject("SUBMIT", MODULE, "PRESCRIPTION", saved.getId(),
                    PrescriptionStatus.DRAFT.name(), saved.getStatus().name());
        }
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse updateDraft(Long id, PrescriptionRequest req) {
        Prescription p = require(id);
        if (p.getStatus() != PrescriptionStatus.DRAFT) {
            throw new BusinessException("Only DRAFT prescriptions can be edited");
        }
        validateCreate(req);
        applyRequest(p, req);
        applyWithdrawal(p, req.getWithdrawalPeriodDays(),
                p.getTreatmentStartDate() != null ? p.getTreatmentStartDate() : LocalDate.now());
        Prescription saved = prescriptionRepository.save(p);
        auditService.logObject("UPDATE", MODULE, "PRESCRIPTION", saved.getId(), null, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse submit(Long id) {
        Prescription p = require(id);
        if (p.getStatus() != PrescriptionStatus.DRAFT && p.getStatus() != PrescriptionStatus.SUBMITTED) {
            throw new BusinessException("Only DRAFT prescriptions can be submitted");
        }
        String before = p.getStatus().name();
        p.setStatus(PrescriptionStatus.PENDING_PHARMACY);
        if (p.getPrescribedDate() == null) p.setPrescribedDate(LocalDate.now());
        Prescription saved = prescriptionRepository.save(p);
        notifyPharmacists(saved);
        auditService.logObject("SUBMIT", MODULE, "PRESCRIPTION", saved.getId(), before, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse approve(Long id) {
        Prescription p = require(id);
        if (!APPROVABLE.contains(p.getStatus())) {
            throw new BusinessException("Prescription cannot be approved from status " + p.getStatus());
        }
        assertStockAvailable(p);
        String before = summarize(p);
        User pharmacist = currentUser();
        p.setStatus(PrescriptionStatus.APPROVED);
        p.setApprovedBy(pharmacist);
        p.setApprovedAt(LocalDateTime.now());
        Prescription saved = prescriptionRepository.save(p);
        notificationService.createRoleNotification(
                "Prescription Approved",
                "Rx " + saved.getPrescriptionNumber() + " approved by pharmacy.",
                Role.VETERINARIAN);
        if (saved.getCreatedByVet() != null) {
            notificationService.createUserNotification(
                    "Prescription Approved",
                    "Rx " + saved.getPrescriptionNumber() + " was approved.",
                    saved.getCreatedByVet());
        }
        auditService.logObject("APPROVE", MODULE, "PRESCRIPTION", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse reject(Long id, RejectPrescriptionRequest req) {
        Prescription p = require(id);
        if (!APPROVABLE.contains(p.getStatus())) {
            throw new BusinessException("Prescription cannot be rejected from status " + p.getStatus());
        }
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("Rejection reason is required");
        }
        String before = summarize(p);
        p.setStatus(PrescriptionStatus.REJECTED);
        p.setRejectionReason(req.getReason().trim());
        Prescription saved = prescriptionRepository.save(p);
        notificationService.createRoleNotification(
                "Prescription Rejected",
                "Rx " + saved.getPrescriptionNumber() + " rejected: " + saved.getRejectionReason(),
                Role.VETERINARIAN);
        if (saved.getCreatedByVet() != null) {
            notificationService.createUserNotification(
                    "Prescription Rejected",
                    "Rx " + saved.getPrescriptionNumber() + " rejected: " + saved.getRejectionReason(),
                    saved.getCreatedByVet());
        }
        auditService.logObject("REJECT", MODULE, "PRESCRIPTION", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse cancel(Long id) {
        Prescription p = require(id);
        if (!CANCELABLE.contains(p.getStatus())) {
            throw new BusinessException("Prescription cannot be cancelled from status " + p.getStatus());
        }
        String before = summarize(p);
        p.setStatus(PrescriptionStatus.CANCELLED);
        p.setCancelledAt(LocalDateTime.now());
        Prescription saved = prescriptionRepository.save(p);
        auditService.logObject("CANCEL", MODULE, "PRESCRIPTION", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public PrescriptionResponse dispense(Long id, DispensePrescriptionRequest req) {
        Prescription p = require(id);
        if (p.getStatus() == PrescriptionStatus.DISPENSED || p.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new BusinessException("Prescription already dispensed");
        }
        if (p.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new BusinessException("Cancelled prescription cannot be dispensed");
        }
        if (p.getStatus() == PrescriptionStatus.REJECTED) {
            throw new BusinessException("Rejected prescription cannot be dispensed");
        }
        if (!DISPENSABLE.contains(p.getStatus())) {
            throw new BusinessException("Prescription must be APPROVED before dispensing (current: " + p.getStatus() + ")");
        }
        if (p.getInventoryItemId() == null) {
            throw new BusinessException("Prescription is not linked to an inventory item");
        }
        if (p.getQuantity() == null || p.getQuantity() <= 0) {
            throw new BusinessException("Invalid prescription quantity");
        }

        String before = summarize(p);
        User pharmacist = currentUser();
        Long batchId = req != null ? req.getBatchId() : null;

        StockOutRequest stockOut = new StockOutRequest();
        stockOut.setItemId(p.getInventoryItemId());
        stockOut.setQuantity(p.getQuantity());
        stockOut.setReason("VETERINARY_PRESCRIPTION:" + p.getPrescriptionNumber());
        stockOut.setIssuedToType(IssuedToType.FARM);
        stockOut.setFarmId(p.getFarmId());
        stockOut.setReferenceType(INVENTORY_REF);
        stockOut.setReferenceId(p.getId());
        stockOut.setBatchId(batchId);
        stockOut.setMovementTypeOverride("INTERNAL_CONSUMPTION");

        Map<String, Object> stockResult = inventoryService.stockOut(stockOut);
        BigDecimal totalCost = toBigDecimal(stockResult.get("totalCost"));
        BigDecimal unitCost = toBigDecimal(stockResult.get("unitCost"));
        Long usedBatchId = stockResult.get("batchId") instanceof Number n ? n.longValue() : null;
        String lot = stockResult.get("batchNumber") != null ? stockResult.get("batchNumber").toString() : null;
        LocalDate expiry = stockResult.get("expiryDate") instanceof LocalDate d ? d : null;

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            // Internal operational cost — Dr expense, Cr inventory. Never revenue / never cash.
            financeService.postInventoryConsumption(
                    totalCost,
                    BusinessUnit.PHARMACY,
                    "5120",
                    String.format("Vet Rx %s | farm=%s flock=%s | %s x%s | batch=%s",
                            p.getPrescriptionNumber(),
                            p.getFarmId(),
                            p.getFlockId(),
                            p.getDrugName(),
                            p.getQuantity(),
                            lot != null ? lot : "-"),
                    FinanceSourceTypes.MODULE_VETERINARY,
                    FinanceSourceTypes.REF_VET_DISPENSE,
                    p.getId());

            if (p.getFarmId() != null) {
                FarmCostAllocation alloc = new FarmCostAllocation();
                alloc.setFarmId(p.getFarmId());
                alloc.setFlockId(p.getFlockId());
                alloc.setCostType(FarmCostType.DRUG);
                alloc.setAmount(totalCost);
                alloc.setReferenceType(FinanceSourceTypes.REF_VET_DISPENSE);
                alloc.setReferenceId(p.getId());
                alloc.setTransactionDate(LocalDate.now());
                farmCostAllocationRepository.save(alloc);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        p.setDispensedBy(pharmacist);
        p.setDispensedAt(now);
        p.setUnitCostAtDispense(unitCost);
        p.setTotalCostAtDispense(totalCost);
        p.setDispensedLotNumber(lot);
        p.setDispensedExpiry(expiry);
        if (usedBatchId != null) {
            StockBatch batch = stockBatchRepository.findById(usedBatchId).orElse(null);
            p.setDispensedBatch(batch);
        }
        if (req != null && req.getNotes() != null && !req.getNotes().isBlank()) {
            String existing = p.getNotes() != null ? p.getNotes() + "\n" : "";
            p.setNotes(existing + "Dispense note: " + req.getNotes().trim());
        }
        if (p.getTreatmentStartDate() == null) {
            p.setTreatmentStartDate(LocalDate.now());
            if (p.getWithdrawalPeriodDays() != null && p.getWithdrawalPeriodDays() > 0) {
                p.setWithdrawalEndDate(LocalDate.now().plusDays(p.getWithdrawalPeriodDays()));
            }
        }
        p.setStatus(PrescriptionStatus.DISPENSED);
        Prescription dispensed = prescriptionRepository.save(p);

        dispensed.setStatus(PrescriptionStatus.COMPLETED);
        dispensed.setCompletedAt(now);
        Prescription saved = prescriptionRepository.save(dispensed);

        notificationService.createRoleNotification(
                "Prescription Dispensed",
                "Rx " + saved.getPrescriptionNumber() + " dispensed. Stock deducted; cost posted.",
                Role.VETERINARIAN);
        if (saved.getCreatedByVet() != null) {
            notificationService.createUserNotification(
                    "Prescription Dispensed",
                    "Rx " + saved.getPrescriptionNumber() + " has been dispensed.",
                    saved.getCreatedByVet());
        }
        notificationService.createRoleNotification(
                "Veterinary Expense Recorded",
                String.format("ETB %s posted for Rx %s", totalCost, saved.getPrescriptionNumber()),
                Role.FINANCE);
        auditService.logObject("DISPENSE", MODULE, "PRESCRIPTION", saved.getId(), before, toResponse(saved));
        auditService.logObject("COMPLETE", MODULE, "PRESCRIPTION", saved.getId(),
                PrescriptionStatus.DISPENSED.name(), PrescriptionStatus.COMPLETED.name());
        return toResponse(saved);
    }

    public boolean isFlockUnderWithdrawal(Long flockId) {
        if (flockId == null) return false;
        return prescriptionRepository.existsByFlockIdAndWithdrawalEndDateAfter(flockId, LocalDate.now().minusDays(1));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validateCreate(PrescriptionRequest req) {
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }
        if (req.getInventoryItemId() != null) {
            inventoryItemRepository.findById(req.getInventoryItemId())
                    .orElseThrow(() -> new BusinessException("Inventory item not found"));
        }
    }

    private void applyRequest(Prescription p, PrescriptionRequest req) {
        p.setPrescriptionNumber(req.getPrescriptionNumber());
        p.setPrescriptionType(req.getPrescriptionType() != null ? req.getPrescriptionType() : PrescriptionType.INTERNAL_FARM);
        p.setFarmId(req.getFarmId());
        p.setBarnId(req.getBarnId());
        p.setFlockId(req.getFlockId());
        p.setClientId(req.getClientId());
        p.setDiseaseCaseId(req.getDiseaseCaseId());
        p.setInventoryItemId(req.getInventoryItemId());
        p.setDrugName(req.getDrugName());
        p.setQuantity(req.getQuantity());
        p.setUnit(req.getUnit());
        p.setDosageInstruction(req.getDosageInstruction());
        p.setFrequency(req.getFrequency());
        p.setDurationDays(req.getDurationDays());
        p.setAdministrationRoute(req.getAdministrationRoute());
        p.setPriority(req.getPriority() != null ? req.getPriority() : PrescriptionPriority.NORMAL);
        p.setNotes(req.getNotes());
        p.setWithdrawalPeriodDays(req.getWithdrawalPeriodDays());
    }

    private void applyWithdrawal(Prescription p, Integer days, LocalDate start) {
        p.setWithdrawalPeriodDays(days);
        if (days != null && days > 0) {
            p.setTreatmentStartDate(start);
            p.setWithdrawalEndDate(start.plusDays(days));
        } else {
            p.setWithdrawalEndDate(null);
        }
    }

    private void assertStockAvailable(Prescription p) {
        if (p.getInventoryItemId() == null || p.getQuantity() == null) return;
        InventoryItem item = inventoryItemRepository.findById(p.getInventoryItemId()).orElse(null);
        if (item != null && item.getCurrentStock() != null && item.getCurrentStock() < p.getQuantity()) {
            throw new BusinessException("Insufficient stock for " + item.getItemName()
                    + " (available=" + item.getCurrentStock() + ", required=" + p.getQuantity() + ")");
        }
    }

    private void notifyPharmacists(Prescription p) {
        String msg = String.format("Rx %s | vet=%s | farm=%s | flock=%s | priority=%s | items=1 | %s",
                p.getPrescriptionNumber(),
                p.getCreatedByVet() != null ? p.getCreatedByVet().getFullName() : "n/a",
                p.getFarmId(),
                p.getFlockId(),
                p.getPriority(),
                p.getCreatedAt() != null ? p.getCreatedAt() : LocalDateTime.now());
        notificationService.createRoleNotification("New Prescription Pending", msg, Role.PHARMACIST);
    }

    private Prescription require(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found"));
    }

    private String summarize(Prescription p) {
        return p.getPrescriptionNumber() + " | " + p.getStatus() + " | " + p.getDrugName() + " x" + p.getQuantity();
    }

    private int priorityRank(PrescriptionPriority p) {
        if (p == null) return 0;
        return switch (p) {
            case URGENT -> 4;
            case HIGH -> 3;
            case NORMAL -> 2;
            case LOW -> 1;
        };
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private PrescriptionResponse toResponse(Prescription p) {
        return toResponse(p, false);
    }

    private PrescriptionResponse toResponse(Prescription p, boolean includeStock) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.setId(p.getId());
        r.setPrescriptionNumber(p.getPrescriptionNumber());
        r.setPrescriptionType(p.getPrescriptionType());
        r.setFarmId(p.getFarmId());
        if (p.getFarmId() != null) {
            farmRepository.findById(p.getFarmId()).ifPresent(f -> r.setFarmName(f.getFarmName()));
        }
        r.setBarnId(p.getBarnId());
        r.setFlockId(p.getFlockId());
        if (p.getFlockId() != null) {
            flockRepository.findById(p.getFlockId()).ifPresent(f -> {
                r.setBatchCode(f.getBatchCode());
                r.setFlockName(f.getBatchCode());
            });
        }
        r.setClientId(p.getClientId());
        r.setDiseaseCaseId(p.getDiseaseCaseId());
        r.setInventoryItemId(p.getInventoryItemId());
        r.setDrugName(p.getDrugName());
        r.setQuantity(p.getQuantity());
        r.setUnit(p.getUnit());
        r.setDosageInstruction(p.getDosageInstruction());
        r.setFrequency(p.getFrequency());
        r.setDurationDays(p.getDurationDays());
        r.setAdministrationRoute(p.getAdministrationRoute());
        r.setPriority(p.getPriority());
        r.setNotes(p.getNotes());
        r.setRejectionReason(p.getRejectionReason());
        r.setPrescribedDate(p.getPrescribedDate());
        r.setWithdrawalPeriodDays(p.getWithdrawalPeriodDays());
        r.setTreatmentStartDate(p.getTreatmentStartDate());
        r.setWithdrawalEndDate(p.getWithdrawalEndDate());
        r.setFlockUnderWithdrawal(isFlockUnderWithdrawal(p.getFlockId()));
        if (p.getCreatedByVet() != null) {
            r.setPrescribedById(p.getCreatedByVet().getId());
            r.setCreatedByVet(p.getCreatedByVet().getFullName());
        }
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        if (p.getApprovedBy() != null) r.setApprovedByName(p.getApprovedBy().getFullName());
        r.setApprovedAt(p.getApprovedAt());
        if (p.getDispensedBy() != null) r.setDispensedByName(p.getDispensedBy().getFullName());
        r.setDispensedAt(p.getDispensedAt());
        r.setCompletedAt(p.getCompletedAt());
        r.setDispensedBatchId(p.getDispensedBatchId());
        r.setDispensedLotNumber(p.getDispensedLotNumber());
        r.setDispensedExpiry(p.getDispensedExpiry());
        r.setUnitCostAtDispense(p.getUnitCostAtDispense());
        r.setTotalCostAtDispense(p.getTotalCostAtDispense());

        if (includeStock && p.getInventoryItemId() != null) {
            inventoryItemRepository.findById(p.getInventoryItemId()).ifPresent(item -> {
                r.setAvailableStock(item.getCurrentStock());
            });
            List<Map<String, Object>> batches = new ArrayList<>();
            for (StockBatch b : stockBatchRepository.findAvailableBatchesFEFO(p.getInventoryItemId(), LocalDate.now())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", b.getId());
                m.put("batchNumber", b.getBatchNumber());
                m.put("quantityRemaining", b.getQuantityRemaining());
                m.put("expiryDate", b.getExpiryDate());
                m.put("unitCost", b.getUnitCost());
                batches.add(m);
            }
            r.setAvailableBatches(batches);
        }
        return r;
    }
}
