package com.trustagro.farm.service;

import com.trustagro.farm.entity.FarmNumberSequence;
import com.trustagro.farm.repository.FarmNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FarmNumberService {

    public static final String SEQ_IDR = "IDR";
    public static final String SEQ_PV = "PV";
    public static final String SEQ_CD = "CD";
    public static final String SEQ_BR = "BR";

    private final FarmNumberSequenceRepository sequenceRepository;

    @Transactional
    public String next(String sequenceName, String prefix) {
        FarmNumberSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> {
                    FarmNumberSequence created = new FarmNumberSequence();
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
    public String nextInternalDeliveryReceipt() {
        return next(SEQ_IDR, "IDR");
    }

    @Transactional
    public String nextPaymentVerification() {
        return next(SEQ_PV, "PV");
    }

    @Transactional
    public String nextChickenDispatch() {
        return next(SEQ_CD, "CD");
    }

    @Transactional
    public String nextBatchRegistration() {
        return next(SEQ_BR, "BR");
    }
}
