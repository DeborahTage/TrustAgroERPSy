package com.trustagro.crm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.CrmLeadStatus;
import com.trustagro.crm.entity.CustomerOrderStatus;
import com.trustagro.crm.entity.OpportunityStage;
import com.trustagro.crm.entity.QuotationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure transition-matrix tests for CRM commercial lifecycles.
 */
class CrmLifecycleRulesTest {

    // ─── Lead ────────────────────────────────────────────────────

    @Test
    void leadHappyPathToConverted() {
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.NEW, CrmLeadStatus.CONTACTED));
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.CONTACTED, CrmLeadStatus.QUALIFIED));
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.QUALIFIED, CrmLeadStatus.CONVERTED));
    }

    @Test
    void leadMaySkipContactedToQualified() {
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.NEW, CrmLeadStatus.QUALIFIED));
    }

    @Test
    void leadMayExitToUnqualifiedOrLost() {
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.NEW, CrmLeadStatus.UNQUALIFIED));
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.CONTACTED, CrmLeadStatus.LOST));
        assertDoesNotThrow(() -> LeadLifecycle.assertTransition(CrmLeadStatus.QUALIFIED, CrmLeadStatus.LOST));
    }

    @Test
    void leadTerminalStatusesRejectFurtherTransitions() {
        assertThrows(BusinessException.class, () ->
                LeadLifecycle.assertTransition(CrmLeadStatus.CONVERTED, CrmLeadStatus.QUALIFIED));
        assertThrows(BusinessException.class, () ->
                LeadLifecycle.assertTransition(CrmLeadStatus.LOST, CrmLeadStatus.NEW));
        assertThrows(BusinessException.class, () ->
                LeadLifecycle.assertTransition(CrmLeadStatus.UNQUALIFIED, CrmLeadStatus.CONTACTED));
    }

    @Test
    void leadCannotJumpBackwards() {
        assertThrows(BusinessException.class, () ->
                LeadLifecycle.assertTransition(CrmLeadStatus.QUALIFIED, CrmLeadStatus.NEW));
        assertThrows(BusinessException.class, () ->
                LeadLifecycle.assertTransition(CrmLeadStatus.CONTACTED, CrmLeadStatus.NEW));
    }

    // ─── Opportunity ─────────────────────────────────────────────

    @Test
    void opportunityHappyPathToWon() {
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, OpportunityStage.QUALIFICATION));
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.QUALIFICATION, OpportunityStage.PROPOSAL));
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION));
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.NEGOTIATION, OpportunityStage.WON));
    }

    @Test
    void opportunityMaySkipToProposalOrWon() {
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, OpportunityStage.PROPOSAL));
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROPOSAL, OpportunityStage.WON));
    }

    @Test
    void opportunityMayBeLostFromOpenStages() {
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, OpportunityStage.LOST));
        assertDoesNotThrow(() ->
                OpportunityLifecycle.assertTransition(OpportunityStage.NEGOTIATION, OpportunityStage.LOST));
    }

    @Test
    void opportunityInvalidTransitionsRejected() {
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, OpportunityStage.WON));
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, OpportunityStage.NEGOTIATION));
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(OpportunityStage.WON, OpportunityStage.NEGOTIATION));
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(OpportunityStage.LOST, OpportunityStage.PROSPECTING));
    }

    @Test
    void opportunityNullStagesRejected() {
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(null, OpportunityStage.QUALIFICATION));
        assertThrows(BusinessException.class, () ->
                OpportunityLifecycle.assertTransition(OpportunityStage.PROSPECTING, null));
    }

    // ─── Quotation ───────────────────────────────────────────────

    @Test
    void quotationHappyPathToAccepted() {
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.SUBMITTED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.SUBMITTED, QuotationStatus.APPROVED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.APPROVED, QuotationStatus.SENT));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.SENT, QuotationStatus.ACCEPTED));
    }

    @Test
    void quotationRejectCancelAndExpiryAllowedFromOpenStatuses() {
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.SUBMITTED, QuotationStatus.REJECTED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.SENT, QuotationStatus.REJECTED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.CANCELLED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.APPROVED, QuotationStatus.CANCELLED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.APPROVED, QuotationStatus.EXPIRED));
        assertDoesNotThrow(() ->
                QuotationLifecycle.assertTransition(QuotationStatus.SENT, QuotationStatus.EXPIRED));
    }

    @Test
    void quotationTerminalAndInvalidTransitionsRejected() {
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.ACCEPTED));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.ACCEPTED, QuotationStatus.SENT));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.REJECTED, QuotationStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.EXPIRED, QuotationStatus.SENT));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.CANCELLED, QuotationStatus.DRAFT));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.EXPIRED));
        assertThrows(BusinessException.class, () ->
                QuotationLifecycle.assertTransition(QuotationStatus.DRAFT, QuotationStatus.REJECTED));
    }

    // ─── Customer order ──────────────────────────────────────────

    @Test
    void orderHappyPathToFulfilled() {
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.DRAFT, CustomerOrderStatus.CONFIRMED));
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.CONFIRMED, CustomerOrderStatus.IN_PROGRESS));
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.IN_PROGRESS, CustomerOrderStatus.PARTIALLY_FULFILLED));
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.PARTIALLY_FULFILLED, CustomerOrderStatus.FULFILLED));
    }

    @Test
    void orderMaySkipDirectlyToFulfilledFromConfirmed() {
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.CONFIRMED, CustomerOrderStatus.FULFILLED));
    }

    @Test
    void orderMayCancelFromOpenStatuses() {
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.DRAFT, CustomerOrderStatus.CANCELLED));
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.CONFIRMED, CustomerOrderStatus.CANCELLED));
        assertDoesNotThrow(() ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.IN_PROGRESS, CustomerOrderStatus.CANCELLED));
    }

    @Test
    void orderTerminalAndInvalidTransitionsRejected() {
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.DRAFT, CustomerOrderStatus.FULFILLED));
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.FULFILLED, CustomerOrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.CANCELLED, CustomerOrderStatus.DRAFT));
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.DRAFT, CustomerOrderStatus.IN_PROGRESS));
    }

    @Test
    void nullOrderStatusesRejected() {
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(null, CustomerOrderStatus.CONFIRMED));
        assertThrows(BusinessException.class, () ->
                CustomerOrderLifecycle.assertTransition(CustomerOrderStatus.DRAFT, null));
    }
}
