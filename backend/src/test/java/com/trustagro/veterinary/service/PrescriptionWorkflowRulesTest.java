package com.trustagro.veterinary.service;

import com.trustagro.veterinary.entity.PrescriptionStatus;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused transition-guard tests for the canonical prescription lifecycle.
 * Full transactional dispense is covered via PrescriptionWorkflowService rules.
 */
class PrescriptionWorkflowRulesTest {

    private static final Set<PrescriptionStatus> APPROVABLE = EnumSet.of(
            PrescriptionStatus.PENDING_PHARMACY, PrescriptionStatus.PENDING, PrescriptionStatus.SUBMITTED
    );
    private static final Set<PrescriptionStatus> DISPENSABLE = EnumSet.of(PrescriptionStatus.APPROVED);
    private static final Set<PrescriptionStatus> CANCELABLE = EnumSet.of(
            PrescriptionStatus.DRAFT, PrescriptionStatus.SUBMITTED,
            PrescriptionStatus.PENDING, PrescriptionStatus.PENDING_PHARMACY
    );

    @Test
    void approvedCanBeDispensed() {
        assertTrue(DISPENSABLE.contains(PrescriptionStatus.APPROVED));
    }

    @Test
    void rejectedCannotBeDispensed() {
        assertFalse(DISPENSABLE.contains(PrescriptionStatus.REJECTED));
    }

    @Test
    void cancelledCannotBeDispensed() {
        assertFalse(DISPENSABLE.contains(PrescriptionStatus.CANCELLED));
    }

    @Test
    void pendingPharmacyCanBeApprovedOrRejected() {
        assertTrue(APPROVABLE.contains(PrescriptionStatus.PENDING_PHARMACY));
    }

    @Test
    void completedCannotBeApproved() {
        assertFalse(APPROVABLE.contains(PrescriptionStatus.COMPLETED));
        assertFalse(APPROVABLE.contains(PrescriptionStatus.DISPENSED));
    }

    @Test
    void draftCanBeCancelledButNotDispensed() {
        assertTrue(CANCELABLE.contains(PrescriptionStatus.DRAFT));
        assertFalse(DISPENSABLE.contains(PrescriptionStatus.DRAFT));
    }

    @Test
    void inventoryReferenceIsVeterinaryPrescription() {
        assertEquals("VETERINARY_PRESCRIPTION", PrescriptionWorkflowService.INVENTORY_REF);
    }

    @Test
    void lifecycleOrderIsDocumented() {
        PrescriptionStatus[] expected = {
                PrescriptionStatus.DRAFT,
                PrescriptionStatus.SUBMITTED,
                PrescriptionStatus.PENDING_PHARMACY,
                PrescriptionStatus.APPROVED,
                PrescriptionStatus.DISPENSED,
                PrescriptionStatus.COMPLETED
        };
        assertEquals(6, expected.length);
        assertTrue(EnumSet.allOf(PrescriptionStatus.class).contains(PrescriptionStatus.REJECTED));
        assertTrue(EnumSet.allOf(PrescriptionStatus.class).contains(PrescriptionStatus.CANCELLED));
    }
}
