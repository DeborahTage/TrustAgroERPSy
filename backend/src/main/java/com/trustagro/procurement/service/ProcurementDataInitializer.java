package com.trustagro.procurement.service;

import com.trustagro.procurement.entity.ProcNumberSequence;
import com.trustagro.procurement.repository.ProcNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementDataInitializer implements ApplicationRunner {

    private final ProcNumberSequenceRepository sequenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(ProcNumberService.SEQ_SUP);
        seed(ProcNumberService.SEQ_REQ);
        seed(ProcNumberService.SEQ_PO);
        seed(ProcNumberService.SEQ_GRN);
        seed(ProcNumberService.SEQ_SINV);
        seed(ProcNumberService.SEQ_SPAY);
        log.info("Procurement number sequences ready");
    }

    private void seed(String name) {
        if (sequenceRepository.findById(name).isEmpty()) {
            ProcNumberSequence seq = new ProcNumberSequence();
            seq.setName(name);
            seq.setNextValue(1L);
            sequenceRepository.save(seq);
        }
    }
}
