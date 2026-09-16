package com.trustagro.procurement.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.inventory.entity.PurchaseOrderStatus;
import com.trustagro.procurement.entity.GoodsReceiptStatus;
import com.trustagro.procurement.entity.ProcRequisitionStatus;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcurementLifecycleRulesTest {

    // ─── Requisition ─────────────────────────────────────────────

    @Test
    void requisitionDraftToSubmittedAllowed() {
        assertDoesNotThrow(() ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.DRAFT, ProcRequisitionStatus.SUBMITTED));
    }

    @Test
    void requisitionSubmittedToApprovedOrReviewAllowed() {
        assertDoesNotThrow(() ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.SUBMITTED, ProcRequisitionStatus.APPROVED));
        assertDoesNotThrow(() ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.SUBMITTED, ProcRequisitionStatus.UNDER_REVIEW));
        assertDoesNotThrow(() ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.UNDER_REVIEW, ProcRequisitionStatus.APPROVED));
    }

    @Test
    void requisitionApprovedToConvertedAllowed() {
        assertDoesNotThrow(() ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.APPROVED, ProcRequisitionStatus.CONVERTED));
    }

    @Test
    void requisitionConvertedAndRejectedAreTerminal() {
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.CONVERTED, ProcRequisitionStatus.CANCELLED));
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.REJECTED, ProcRequisitionStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.CANCELLED, ProcRequisitionStatus.DRAFT));
    }

    @Test
    void requisitionCannotSkipToConvertedFromDraft() {
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.DRAFT, ProcRequisitionStatus.CONVERTED));
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(
                        ProcRequisitionStatus.SUBMITTED, ProcRequisitionStatus.CONVERTED));
    }

    // ─── Purchase order ──────────────────────────────────────────

    @Test
    void purchaseOrderHappyPathDraftToSent() {
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.SUBMITTED));
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.SUBMITTED, PurchaseOrderStatus.APPROVED));
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.SENT));
    }

    @Test
    void purchaseOrderPartialThenFullyReceived() {
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.SENT, PurchaseOrderStatus.PARTIALLY_RECEIVED));
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.PARTIALLY_RECEIVED));
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.FULLY_RECEIVED));
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.SENT, PurchaseOrderStatus.FULLY_RECEIVED));
    }

    @Test
    void purchaseOrderFullyReceivedThenClosed() {
        assertDoesNotThrow(() ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.FULLY_RECEIVED, PurchaseOrderStatus.CLOSED));
        assertThrows(BusinessException.class, () ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.CLOSED, PurchaseOrderStatus.SENT));
    }

    @Test
    void purchaseOrderCannotSkipApproval() {
        assertThrows(BusinessException.class, () ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.SUBMITTED, PurchaseOrderStatus.SENT));
    }

    @Test
    void purchaseOrderCancelledIsTerminal() {
        assertThrows(BusinessException.class, () ->
                PurchaseOrderLifecycle.assertTransition(
                        PurchaseOrderStatus.CANCELLED, PurchaseOrderStatus.DRAFT));
    }

    // ─── Goods receipt ───────────────────────────────────────────

    @Test
    void goodsReceiptDraftToPostedOrCancelled() {
        assertDoesNotThrow(() ->
                GoodsReceiptLifecycle.assertTransition(
                        GoodsReceiptStatus.DRAFT, GoodsReceiptStatus.POSTED));
        assertDoesNotThrow(() ->
                GoodsReceiptLifecycle.assertTransition(
                        GoodsReceiptStatus.DRAFT, GoodsReceiptStatus.CANCELLED));
    }

    @Test
    void goodsReceiptPostedIsTerminal() {
        assertThrows(BusinessException.class, () ->
                GoodsReceiptLifecycle.assertTransition(
                        GoodsReceiptStatus.POSTED, GoodsReceiptStatus.CANCELLED));
        assertThrows(BusinessException.class, () ->
                GoodsReceiptLifecycle.assertTransition(
                        GoodsReceiptStatus.POSTED, GoodsReceiptStatus.DRAFT));
    }

    // ─── Supplier invoice ────────────────────────────────────────

    @Test
    void supplierInvoiceMatchApprovePayPath() {
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.DRAFT, SupplierInvoiceStatus.SUBMITTED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.SUBMITTED, SupplierInvoiceStatus.MATCHED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.MATCHED, SupplierInvoiceStatus.APPROVED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.APPROVED, SupplierInvoiceStatus.PARTIALLY_PAID));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.PARTIALLY_PAID, SupplierInvoiceStatus.PAID));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.APPROVED, SupplierInvoiceStatus.PAID));
    }

    @Test
    void supplierInvoiceMismatchedCanRematch() {
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.SUBMITTED, SupplierInvoiceStatus.MISMATCHED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.MISMATCHED, SupplierInvoiceStatus.SUBMITTED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.MISMATCHED, SupplierInvoiceStatus.MATCHED));
        assertDoesNotThrow(() ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.MISMATCHED, SupplierInvoiceStatus.MISMATCHED));
    }

    @Test
    void supplierInvoiceCannotApproveWhenMismatchedOrSkipMatch() {
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.MISMATCHED, SupplierInvoiceStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.SUBMITTED, SupplierInvoiceStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.DRAFT, SupplierInvoiceStatus.MATCHED));
    }

    @Test
    void supplierInvoicePaidIsTerminal() {
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.PAID, SupplierInvoiceStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(
                        SupplierInvoiceStatus.PAID, SupplierInvoiceStatus.PARTIALLY_PAID));
    }

    @Test
    void nullTransitionsRejected() {
        assertThrows(BusinessException.class, () ->
                ProcRequisitionLifecycle.assertTransition(null, ProcRequisitionStatus.SUBMITTED));
        assertThrows(BusinessException.class, () ->
                PurchaseOrderLifecycle.assertTransition(PurchaseOrderStatus.DRAFT, null));
        assertThrows(BusinessException.class, () ->
                GoodsReceiptLifecycle.assertTransition(null, GoodsReceiptStatus.POSTED));
        assertThrows(BusinessException.class, () ->
                SupplierInvoiceLifecycle.assertTransition(SupplierInvoiceStatus.DRAFT, null));
    }
}
