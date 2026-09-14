package com.trustagro.veterinary.service;

import com.trustagro.user.entity.User;
import com.trustagro.veterinary.dto.DispensePrescriptionRequest;
import com.trustagro.veterinary.dto.PrescriptionResponse;
import com.trustagro.veterinary.dto.RejectPrescriptionRequest;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @deprecated Use {@link PrescriptionWorkflowService}. Kept as a thin compatibility
 * facade for {@code /api/prescriptions/*} callers during consolidation.
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionWorkflowService workflowService;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional
    public Prescription submitPrescription(Long id) {
        workflowService.submit(id);
        return prescriptionRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Prescription approvePrescription(Long id) {
        workflowService.approve(id);
        return prescriptionRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Prescription dispensePrescription(Long id, User dispenser) {
        workflowService.dispense(id, new DispensePrescriptionRequest());
        return prescriptionRepository.findById(id).orElseThrow();
    }

    public List<Prescription> getPendingPrescriptions() {
        return prescriptionRepository.findByStatus(PrescriptionStatus.PENDING_PHARMACY);
    }

    public PrescriptionResponse reject(Long id, RejectPrescriptionRequest req) {
        return workflowService.reject(id, req);
    }
}
