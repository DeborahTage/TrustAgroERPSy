package com.trustagro.procurement.service;

import com.trustagro.procurement.entity.ProcNumberSequence;
import com.trustagro.procurement.repository.ProcNumberSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests verifying that {@link ProcurementDataInitializer}:
 * 1. Creates initial number sequences with nextValue = 1.
 * 2. Preserves advanced sequence numbers on repeated restarts without overwriting.
 */
@ExtendWith(MockitoExtension.class)
class ProcurementDataInitializerTest {

    @Mock
    private ProcNumberSequenceRepository sequenceRepository;

    private final Map<String, ProcNumberSequence> database = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(sequenceRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(database.get(inv.getArgument(0))));
        lenient().when(sequenceRepository.save(any(ProcNumberSequence.class))).thenAnswer(inv -> {
            ProcNumberSequence seq = inv.getArgument(0);
            database.put(seq.getName(), seq);
            return seq;
        });
    }

    @Test
    void initialSeedCreatesAllProcurementSequences() {
        ProcurementDataInitializer initializer = new ProcurementDataInitializer(sequenceRepository);
        initializer.run(null);

        assertEquals(6, database.size());
        for (String seqName : new String[]{
                ProcNumberService.SEQ_SUP,
                ProcNumberService.SEQ_REQ,
                ProcNumberService.SEQ_PO,
                ProcNumberService.SEQ_GRN,
                ProcNumberService.SEQ_SINV,
                ProcNumberService.SEQ_SPAY}) {
            ProcNumberSequence seq = database.get(seqName);
            assertNotNull(seq);
            assertEquals(1L, seq.getNextValue());
        }
    }

    @Test
    void repeatedStartupNeverOverwritesExistingBusinessCounters() {
        ProcurementDataInitializer initializer = new ProcurementDataInitializer(sequenceRepository);
        initializer.run(null);

        // Advance purchase order sequence counter to 42 during business activity
        ProcNumberSequence poSeq = database.get(ProcNumberService.SEQ_PO);
        assertNotNull(poSeq);
        poSeq.setNextValue(42L);

        // Advance requisition sequence counter to 99 during business activity
        ProcNumberSequence reqSeq = database.get(ProcNumberService.SEQ_REQ);
        assertNotNull(reqSeq);
        reqSeq.setNextValue(99L);

        // Second startup run
        initializer.run(null);

        // Counters must remain unchanged
        assertEquals(42L, database.get(ProcNumberService.SEQ_PO).getNextValue());
        assertEquals(99L, database.get(ProcNumberService.SEQ_REQ).getNextValue());
        assertEquals(6, database.size());
    }
}
