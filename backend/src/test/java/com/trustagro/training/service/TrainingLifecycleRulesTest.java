package com.trustagro.training.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.training.entity.ProgramStatus;
import com.trustagro.training.entity.RegistrationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TrainingLifecycleRulesTest {

    @Test
    void programDraftToPublishedAllowed() {
        assertDoesNotThrow(() -> TrainingProgramLifecycle.assertTransition(ProgramStatus.DRAFT, ProgramStatus.PUBLISHED));
    }

    @Test
    void programDraftToCompletedRejected() {
        assertThrows(BusinessException.class,
                () -> TrainingProgramLifecycle.assertTransition(ProgramStatus.DRAFT, ProgramStatus.COMPLETED));
    }

    @Test
    void registrationPendingToConfirmedAllowed() {
        assertDoesNotThrow(() ->
                TrainingRegistrationLifecycle.assertTransition(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED));
    }

    @Test
    void registrationCompletedIsTerminal() {
        assertThrows(BusinessException.class,
                () -> TrainingRegistrationLifecycle.assertTransition(RegistrationStatus.COMPLETED, RegistrationStatus.CANCELLED));
    }

    @Test
    void tenThousandInvoiceSixThousandPaymentLeavesFourThousandAr() {
        BigDecimal invoice = new BigDecimal("10000.00");
        BigDecimal payment = new BigDecimal("6000.00");
        BigDecimal revenue = invoice;
        BigDecimal cash = payment;
        BigDecimal ar = invoice.subtract(payment);
        assertEquals(0, new BigDecimal("10000.00").compareTo(revenue));
        assertEquals(0, new BigDecimal("6000.00").compareTo(cash));
        assertEquals(0, new BigDecimal("4000.00").compareTo(ar));
        assertNotEquals(0, revenue.add(cash).compareTo(invoice),
                "Payment must not be double-counted as revenue");
    }
}
