package com.trustagro.hr.service;

import com.trustagro.hr.entity.*;
import com.trustagro.hr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests verifying that {@link HrDataInitializer}:
 * 1. Always initializes required number sequences and preserves advanced sequence counters.
 * 2. Does NOT seed departments, positions, leave types, or placeholder deduction rates when
 *    {@code app.seed.hr-reference-data=false}.
 * 3. Marks placeholder deduction rates as unapproved ({@code approved = false}) with disclaimer
 *    notes when {@code app.seed.hr-reference-data=true}.
 */
@ExtendWith(MockitoExtension.class)
class HrDataInitializerTest {

    @Mock private HrDepartmentRepository departmentRepository;
    @Mock private HrPositionRepository positionRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private PayrollDeductionTypeRepository deductionTypeRepository;
    @Mock private HrNumberSequenceRepository sequenceRepository;

    private final Map<String, HrNumberSequence> seqDb = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(sequenceRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(seqDb.get(inv.getArgument(0))));
        lenient().when(sequenceRepository.save(any(HrNumberSequence.class))).thenAnswer(inv -> {
            HrNumberSequence s = inv.getArgument(0);
            seqDb.put(s.getName(), s);
            return s;
        });
    }

    @Test
    void defaultModeOnlySeedsSequencesAndNeverFictionalMasterData() {
        HrDataInitializer initializer = new HrDataInitializer(
                departmentRepository, positionRepository, leaveTypeRepository,
                deductionTypeRepository, sequenceRepository, false);

        initializer.run(null);

        assertEquals(5, seqDb.size());
        assertTrue(seqDb.containsKey("EMPLOYEE"));
        assertTrue(seqDb.containsKey("LEAVE"));
        assertTrue(seqDb.containsKey("PAYROLL_PERIOD"));
        assertTrue(seqDb.containsKey("PAYROLL_PAYMENT"));
        assertTrue(seqDb.containsKey("PAYSLIP"));

        verifyNoInteractions(departmentRepository);
        verifyNoInteractions(positionRepository);
        verifyNoInteractions(leaveTypeRepository);
        verifyNoInteractions(deductionTypeRepository);
    }

    @Test
    void repeatedStartupPreservesExistingSequenceCounters() {
        HrDataInitializer initializer = new HrDataInitializer(
                departmentRepository, positionRepository, leaveTypeRepository,
                deductionTypeRepository, sequenceRepository, false);

        initializer.run(null);

        // Advance employee and payslip sequence counters during business usage
        seqDb.get("EMPLOYEE").setNextValue(150L);
        seqDb.get("PAYSLIP").setNextValue(500L);

        // Second startup run
        initializer.run(null);

        assertEquals(150L, seqDb.get("EMPLOYEE").getNextValue());
        assertEquals(500L, seqDb.get("PAYSLIP").getNextValue());
        assertEquals(5, seqDb.size());
    }

    @Test
    void explicitDevModeSeedsUnapprovedPlaceholderDeductions() {
        HrDataInitializer initializer = new HrDataInitializer(
                departmentRepository, positionRepository, leaveTypeRepository,
                deductionTypeRepository, sequenceRepository, true);

        when(deductionTypeRepository.existsByCode(any())).thenReturn(false);

        initializer.run(null);

        ArgumentCaptor<PayrollDeductionType> captor = ArgumentCaptor.forClass(PayrollDeductionType.class);
        verify(deductionTypeRepository, times(2)).save(captor.capture());

        List<PayrollDeductionType> deductions = captor.getAllValues();
        assertEquals(2, deductions.size());
        for (PayrollDeductionType d : deductions) {
            assertFalse(d.isApproved(), "Seeded placeholder deduction must NOT be approved");
            assertNotNull(d.getNotes());
            assertTrue(d.getNotes().contains("PLACEHOLDER RATE"), "Seeded deduction must contain placeholder disclaimer");
        }
    }
}
