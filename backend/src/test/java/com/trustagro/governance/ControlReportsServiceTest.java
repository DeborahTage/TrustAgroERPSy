package com.trustagro.governance;

import com.trustagro.audit.service.AuditService;
import com.trustagro.crm.repository.CrmQuotationRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.hr.repository.LeaveRequestRepository;
import com.trustagro.hr.repository.PayrollRunRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.procurement.repository.ProcRequisitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ControlReportsServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock CrmQuotationRepository quotationRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock ProcRequisitionRepository procRequisitionRepository;
    @Mock AuditService auditService;
    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock PayrollRunRepository payrollRunRepository;

    @InjectMocks ControlReportsService service;

    @BeforeEach
    void defaults() {
        when(invoiceRepository.countOpenWithoutSource()).thenReturn(0L);
        when(quotationRepository.countAcceptedWithoutConversion()).thenReturn(0L);
        when(inventoryItemRepository.countNegativeStock()).thenReturn(0L);
        when(purchaseOrderRepository.countByStatus(any())).thenReturn(0L);
        when(procRequisitionRepository.countByStatus(any())).thenReturn(0L);
        when(auditService.failedLoginsSince(any())).thenReturn(0L);
    }

    @Test
    void allOkWhenClean() {
        List<ControlCheckResult> results = service.runAll();
        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(r -> ControlReportsService.OK.equals(r.getStatus())));
    }

    @Test
    void missingInvoiceSourceIsWarningOrError() {
        when(invoiceRepository.countOpenWithoutSource()).thenReturn(2L);
        ControlCheckResult r = service.checkOpenInvoicesWithoutSource();
        assertEquals(ControlReportsService.WARNING, r.getStatus());
        assertEquals(2L, r.getCount());

        when(invoiceRepository.countOpenWithoutSource()).thenReturn(5L);
        assertEquals(ControlReportsService.ERROR, service.checkOpenInvoicesWithoutSource().getStatus());
    }

    @Test
    void unconvertedAcceptedQuotationIsError() {
        when(quotationRepository.countAcceptedWithoutConversion()).thenReturn(1L);
        ControlCheckResult r = service.checkAcceptedQuotationsUnconverted();
        assertEquals(ControlReportsService.ERROR, r.getStatus());
    }

    @Test
    void negativeStockIsError() {
        when(inventoryItemRepository.countNegativeStock()).thenReturn(3L);
        assertEquals(ControlReportsService.ERROR, service.checkNegativeInventory().getStatus());
    }

    @Test
    void pendingProcurementIsWarning() {
        when(purchaseOrderRepository.countByStatus(any())).thenReturn(2L);
        ControlCheckResult r = service.checkPendingProcurementApprovals();
        assertEquals(ControlReportsService.WARNING, r.getStatus());
        assertTrue(r.getCount() >= 2);
    }

    @Test
    void failedLoginsAreWarning() {
        when(auditService.failedLoginsSince(any())).thenReturn(7L);
        ControlCheckResult r = service.checkFailedLogins24h();
        assertEquals(ControlReportsService.WARNING, r.getStatus());
        assertEquals(7L, r.getCount());
    }

    @Test
    void unavailableWhenRepositoryThrows() {
        when(inventoryItemRepository.countNegativeStock()).thenThrow(new RuntimeException("db down"));
        ControlCheckResult r = service.checkNegativeInventory();
        assertEquals(ControlReportsService.UNAVAILABLE, r.getStatus());
    }
}
