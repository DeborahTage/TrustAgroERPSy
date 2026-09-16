package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultancyLifecycleRulesTest {

    @Test
    void engagementDraftToProposedAllowed() {
        assertDoesNotThrow(() ->
                EngagementLifecycle.assertTransition(EngagementStatus.DRAFT, EngagementStatus.PROPOSED));
    }

    @Test
    void engagementDraftToActiveRejected() {
        assertThrows(BusinessException.class, () ->
                EngagementLifecycle.assertTransition(EngagementStatus.DRAFT, EngagementStatus.ACTIVE));
    }

    @Test
    void engagementActiveToOnHoldAndResume() {
        assertDoesNotThrow(() ->
                EngagementLifecycle.assertTransition(EngagementStatus.ACTIVE, EngagementStatus.ON_HOLD));
        assertDoesNotThrow(() ->
                EngagementLifecycle.assertTransition(EngagementStatus.ON_HOLD, EngagementStatus.ACTIVE));
    }

    @Test
    void engagementCompletedIsTerminal() {
        assertThrows(BusinessException.class, () ->
                EngagementLifecycle.assertTransition(EngagementStatus.COMPLETED, EngagementStatus.ACTIVE));
    }

    @Test
    void requestNewToAssignedAllowed() {
        assertDoesNotThrow(() ->
                RequestLifecycle.assertTransition(RequestStatus.NEW, RequestStatus.ASSIGNED));
    }

    @Test
    void requestConvertedIsTerminal() {
        assertThrows(BusinessException.class, () ->
                RequestLifecycle.assertTransition(RequestStatus.CONVERTED, RequestStatus.CANCELLED));
    }

    @Test
    void proposalDraftToSentAllowed() {
        assertDoesNotThrow(() ->
                ProposalLifecycle.assertTransition(ProposalStatus.DRAFT, ProposalStatus.SENT));
    }

    @Test
    void proposalAcceptedIsTerminal() {
        assertThrows(BusinessException.class, () ->
                ProposalLifecycle.assertTransition(ProposalStatus.ACCEPTED, ProposalStatus.SENT));
    }

    @Test
    void deliverableSubmitApproveAndRejectRework() {
        assertDoesNotThrow(() ->
                DeliverableLifecycle.assertTransition(DeliverableStatus.PENDING, DeliverableStatus.IN_PROGRESS));
        assertDoesNotThrow(() ->
                DeliverableLifecycle.assertTransition(DeliverableStatus.IN_PROGRESS, DeliverableStatus.SUBMITTED));
        assertDoesNotThrow(() ->
                DeliverableLifecycle.assertTransition(DeliverableStatus.SUBMITTED, DeliverableStatus.APPROVED));
        assertDoesNotThrow(() ->
                DeliverableLifecycle.assertTransition(DeliverableStatus.SUBMITTED, DeliverableStatus.REJECTED));
        assertDoesNotThrow(() ->
                DeliverableLifecycle.assertTransition(DeliverableStatus.REJECTED, DeliverableStatus.IN_PROGRESS));
    }

    @Test
    void activityPlannedToCompletedAllowed() {
        assertDoesNotThrow(() ->
                ActivityLifecycle.assertTransition(ActivityStatus.PLANNED, ActivityStatus.COMPLETED));
    }

    @Test
    void contractDraftToPendingThenActive() {
        assertDoesNotThrow(() ->
                ContractLifecycle.assertTransition(ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE));
        assertDoesNotThrow(() ->
                ContractLifecycle.assertTransition(ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE));
    }
}
