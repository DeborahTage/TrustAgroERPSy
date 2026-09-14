package com.trustagro.farm.sale.service;

import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChickenSaleLifecycleTest {

    @Test
    void draftCanConfirmOrCancel() {
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.DRAFT, ChickenSaleStatus.CONFIRMED));
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.DRAFT, ChickenSaleStatus.CANCELLED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.DRAFT, ChickenSaleStatus.INVOICED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.DRAFT, ChickenSaleStatus.PAID));
    }

    @Test
    void confirmedCanInvoiceOrCancel() {
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.CONFIRMED, ChickenSaleStatus.INVOICED));
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.CONFIRMED, ChickenSaleStatus.CANCELLED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.CONFIRMED, ChickenSaleStatus.PAID));
    }

    @Test
    void invoicedCannotBeCancelled() {
        assertFalse(ChickenSaleLifecycle.isCancellable(ChickenSaleStatus.INVOICED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.INVOICED, ChickenSaleStatus.CANCELLED));
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.INVOICED, ChickenSaleStatus.PARTIALLY_PAID));
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.INVOICED, ChickenSaleStatus.PAID));
    }

    @Test
    void paidCanCompleteOnly() {
        assertTrue(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.PAID, ChickenSaleStatus.COMPLETED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.PAID, ChickenSaleStatus.CANCELLED));
        assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.COMPLETED, ChickenSaleStatus.PAID));
    }

    @Test
    void cancelledIsTerminal() {
        for (ChickenSaleStatus to : ChickenSaleStatus.values()) {
            assertFalse(ChickenSaleLifecycle.isAllowed(ChickenSaleStatus.CANCELLED, to));
        }
    }

    @Test
    void onlyDraftIsEditable() {
        assertTrue(ChickenSaleLifecycle.isEditable(ChickenSaleStatus.DRAFT));
        assertFalse(ChickenSaleLifecycle.isEditable(ChickenSaleStatus.CONFIRMED));
    }

    @Test
    void invalidTransitionThrows() {
        var ex = assertThrows(com.trustagro.common.exception.BusinessException.class,
                () -> ChickenSaleLifecycle.assertTransition(ChickenSaleStatus.DRAFT, ChickenSaleStatus.PAID));
        assertTrue(ex.getMessage().contains("Invalid chicken sale status transition"));
    }
}
