package com.trustagro.procurement.service;

import com.trustagro.procurement.entity.ProcNumberSequence;
import com.trustagro.procurement.repository.ProcNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcNumberService {

    public static final String SEQ_SUP = "SUP";
    public static final String SEQ_REQ = "REQ";
    public static final String SEQ_PO = "PO";
    public static final String SEQ_GRN = "GRN";
    public static final String SEQ_SINV = "SINV";
    public static final String SEQ_SPAY = "SPAY";
    public static final String SEQ_MR = "MR";
    public static final String SEQ_FA = "FA";
    public static final String SEQ_SI = "SI";
    public static final String SEQ_MD = "MD";
    public static final String SEQ_MSR = "MSR";

    private final ProcNumberSequenceRepository sequenceRepository;

    @Transactional
    public String next(String sequenceName, String prefix) {
        ProcNumberSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> {
                    ProcNumberSequence created = new ProcNumberSequence();
                    created.setName(sequenceName);
                    created.setNextValue(1L);
                    return sequenceRepository.save(created);
                });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        sequenceRepository.save(seq);
        return String.format("%s-%06d", prefix, value);
    }

    @Transactional
    public String nextSupplierCode() {
        return next(SEQ_SUP, "SUP");
    }

    @Transactional
    public String nextRequisitionNumber() {
        return next(SEQ_REQ, "REQ");
    }

    @Transactional
    public String nextPoNumber() {
        return next(SEQ_PO, "PO");
    }

    @Transactional
    public String nextGrnNumber() {
        return next(SEQ_GRN, "GRN");
    }

    @Transactional
    public String nextSupplierInvoiceNumber() {
        return next(SEQ_SINV, "SINV");
    }

    @Transactional
    public String nextPaymentNumber() {
        return next(SEQ_SPAY, "SPAY");
    }

    @Transactional
    public String nextMaterialRequest() {
        return next(SEQ_MR, "MR");
    }

    @Transactional
    public String nextFinanceApproval() {
        return next(SEQ_FA, "FA");
    }

    @Transactional
    public String nextStoreIssue() {
        return next(SEQ_SI, "SI");
    }

    @Transactional
    public String nextMaterialDisbursement() {
        return next(SEQ_MD, "MD");
    }

    @Transactional
    public String nextMaterialStoreReceipt() {
        return next(SEQ_MSR, "MSR");
    }
}
