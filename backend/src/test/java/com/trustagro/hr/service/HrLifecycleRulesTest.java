package com.trustagro.hr.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.hr.entity.LeaveRequestStatus;
import com.trustagro.hr.entity.PayrollPeriodStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HrLifecycleRulesTest {

    // ─── Leave ───────────────────────────────────────────────────

    @Test
    void leavePendingToApproveRejectCancelAllowed() {
        assertDoesNotThrow(() ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED));
        assertDoesNotThrow(() ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.PENDING, LeaveRequestStatus.REJECTED));
        assertDoesNotThrow(() ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.PENDING, LeaveRequestStatus.CANCELLED));
    }

    @Test
    void leaveApprovedIsTerminal() {
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.APPROVED, LeaveRequestStatus.PENDING));
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.APPROVED, LeaveRequestStatus.REJECTED));
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.APPROVED, LeaveRequestStatus.CANCELLED));
    }

    @Test
    void leaveRejectedAndCancelledAreTerminal() {
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.REJECTED, LeaveRequestStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.CANCELLED, LeaveRequestStatus.APPROVED));
    }

    @Test
    void leaveNullTransitionRejected() {
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(null, LeaveRequestStatus.APPROVED));
        assertThrows(BusinessException.class, () ->
                LeaveLifecycle.assertTransition(LeaveRequestStatus.PENDING, null));
    }

    @Test
    void onlyPendingLeaveIsEditable() {
        assertTrue(LeaveLifecycle.isEditable(LeaveRequestStatus.PENDING));
        assertFalse(LeaveLifecycle.isEditable(LeaveRequestStatus.APPROVED));
        assertFalse(LeaveLifecycle.isEditable(LeaveRequestStatus.REJECTED));
        assertFalse(LeaveLifecycle.isEditable(LeaveRequestStatus.CANCELLED));
    }

    // ─── Payroll period ──────────────────────────────────────────

    @Test
    void payrollHappyPathOpenToClosed() {
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.OPEN, PayrollPeriodStatus.CALCULATED));
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.APPROVED));
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.APPROVED, PayrollPeriodStatus.PROCESSED));
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.PROCESSED, PayrollPeriodStatus.CLOSED));
    }

    @Test
    void payrollCannotSkipCalculationOrApproval() {
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.OPEN, PayrollPeriodStatus.APPROVED));
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.OPEN, PayrollPeriodStatus.PROCESSED));
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.PROCESSED));
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.APPROVED, PayrollPeriodStatus.CLOSED));
    }

    @Test
    void payrollRecalculationAllowedOnlyBeforeApproval() {
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.CALCULATED));
        assertTrue(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.OPEN));
        assertTrue(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.CALCULATED));
        assertFalse(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.APPROVED));
        assertFalse(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.PROCESSED));
        assertFalse(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.CLOSED));
        assertFalse(PayrollPeriodLifecycle.isCalculable(PayrollPeriodStatus.CANCELLED));
    }

    @Test
    void payrollCancellableOnlyBeforeApproval() {
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.OPEN, PayrollPeriodStatus.CANCELLED));
        assertDoesNotThrow(() -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.CALCULATED, PayrollPeriodStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.APPROVED, PayrollPeriodStatus.CANCELLED));
        assertThrows(BusinessException.class, () -> PayrollPeriodLifecycle.assertTransition(
                PayrollPeriodStatus.PROCESSED, PayrollPeriodStatus.CANCELLED));
    }

    @Test
    void payrollClosedAndCancelledAreTerminal() {
        for (PayrollPeriodStatus to : PayrollPeriodStatus.values()) {
            assertThrows(BusinessException.class, () ->
                    PayrollPeriodLifecycle.assertTransition(PayrollPeriodStatus.CLOSED, to));
            assertThrows(BusinessException.class, () ->
                    PayrollPeriodLifecycle.assertTransition(PayrollPeriodStatus.CANCELLED, to));
        }
    }

    @Test
    void payrollLockedOncePostedOrCancelled() {
        assertFalse(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.OPEN));
        assertFalse(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.CALCULATED));
        assertFalse(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.APPROVED));
        assertTrue(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.PROCESSED));
        assertTrue(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.CLOSED));
        assertTrue(PayrollPeriodLifecycle.isLocked(PayrollPeriodStatus.CANCELLED));
    }

    @Test
    void payrollPayableOnlyAfterFinancePosting() {
        assertFalse(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.OPEN));
        assertFalse(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.CALCULATED));
        assertFalse(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.APPROVED));
        assertTrue(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.PROCESSED));
        assertTrue(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.CLOSED));
        assertFalse(PayrollPeriodLifecycle.isPayable(PayrollPeriodStatus.CANCELLED));
    }

    @Test
    void payrollNullTransitionRejected() {
        assertThrows(BusinessException.class, () ->
                PayrollPeriodLifecycle.assertTransition(null, PayrollPeriodStatus.CALCULATED));
        assertThrows(BusinessException.class, () ->
                PayrollPeriodLifecycle.assertTransition(PayrollPeriodStatus.OPEN, null));
    }
}
