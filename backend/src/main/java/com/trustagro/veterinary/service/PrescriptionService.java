package com.trustagro.veterinary.service;

import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.MovementType;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.entity.StockMovement;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.notification.entity.Notification;
import com.trustagro.notification.entity.NotificationType;
import com.trustagro.notification.repository.NotificationRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final InventoryItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final FinanceService financeService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Prescription submitPrescription(Long id) {
        Prescription rx = prescriptionRepository.findById(id).orElseThrow();
        rx.setStatus(PrescriptionStatus.PENDING_PHARMACY);
        
        // Notify Pharmacy
        Notification notif = new Notification();
        notif.setTitle("New Prescription Received");
        notif.setMessage("Vet Order " + rx.getPrescriptionNumber() + " submitted for " + rx.getDrugName());
        notif.setType(NotificationType.SYSTEM);
        notif.setTargetRole(Role.PHARMACIST);
        notificationRepository.save(notif);
        
        return prescriptionRepository.save(rx);
    }

    @Transactional
    public Prescription approvePrescription(Long id) {
        Prescription rx = prescriptionRepository.findById(id).orElseThrow();
        
        // Validation: Verify stock availability
        if (rx.getInventoryItem() != null) {
            if (rx.getInventoryItem().getAvailableStock() < rx.getQuantity()) {
                throw new IllegalStateException("Insufficient stock to approve prescription " + rx.getPrescriptionNumber());
            }
            // Logic for reserving stock could go here if needed
        }
        
        rx.setStatus(PrescriptionStatus.APPROVED);
        
        // Notify Vet
        Notification notif = new Notification();
        notif.setTitle("Prescription Approved");
        notif.setMessage("Order " + rx.getPrescriptionNumber() + " has been approved by Pharmacy.");
        notif.setType(NotificationType.SYSTEM);
        notif.setTargetUser(rx.getCreatedByVet());
        notificationRepository.save(notif);
        
        return prescriptionRepository.save(rx);
    }

    @Transactional
    public Prescription dispensePrescription(Long id, User dispenser) {
        Prescription rx = prescriptionRepository.findById(id).orElseThrow();
        if (rx.getStatus() != PrescriptionStatus.APPROVED) {
            throw new IllegalStateException("Prescription must be APPROVED before dispensing.");
        }

        InventoryItem item = rx.getInventoryItem();
        if (item == null) {
            throw new IllegalStateException("Prescription not linked to a specific inventory item.");
        }

        double quantity = rx.getQuantity();
        List<StockBatch> batches = batchRepository.findAvailableBatchesFEFO(item.getId(), LocalDate.now());
        
        double remaining = quantity;
        List<StockMovement> movements = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;

        for (StockBatch batch : batches) {
            if (remaining <= 0) break;
            double deduct = Math.min(remaining, batch.getQuantityRemaining());
            remaining -= deduct;
            batch.setQuantityRemaining(batch.getQuantityRemaining() - deduct);
            batchRepository.save(batch);

            StockMovement mv = new StockMovement();
            mv.setItem(item);
            mv.setBatch(batch);
            mv.setMovementType(MovementType.INTERNAL_CONSUMPTION);
            mv.setQuantity(deduct);
            mv.setUnitCost(batch.getUnitCost());
            BigDecimal lineCost = batch.getUnitCost() != null ? batch.getUnitCost().multiply(BigDecimal.valueOf(deduct)) : BigDecimal.ZERO;
            mv.setTotalCost(lineCost);
            totalCost = totalCost.add(lineCost);
            mv.setReferenceId(rx.getId());
            mv.setCreatedBy(dispenser);
            movements.add(mv);
        }

        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock for dispense during processing.");
        }

        item.setCurrentStock(item.getCurrentStock() - quantity);
        itemRepository.save(item);
        movementRepository.saveAll(movements);

        // Internal expense via finance engine
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postExpense(totalCost, com.trustagro.finance.entity.BusinessUnit.PHARMACY, "5120",
                    "Dispensed Rx " + rx.getPrescriptionNumber() + ": " + item.getItemName() + " x" + quantity,
                    "VETERINARY", "PRESCRIPTION", rx.getId());
        }

        rx.setStatus(PrescriptionStatus.DISPENSED);
        
        // Final Notification to Vet
        Notification notif = new Notification();
        notif.setTitle("Prescription Dispensed");
        notif.setMessage("Order " + rx.getPrescriptionNumber() + " has been dispensed to the farm.");
        notif.setType(NotificationType.SYSTEM);
        notif.setTargetUser(rx.getCreatedByVet());
        notificationRepository.save(notif);

        return prescriptionRepository.save(rx);
    }

    public List<Prescription> getPendingPrescriptions() {
        return prescriptionRepository.findByStatus(PrescriptionStatus.PENDING_PHARMACY);
    }
}
