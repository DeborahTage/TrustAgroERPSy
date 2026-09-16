package com.trustagro.governance;

import com.trustagro.audit.service.AuditService;
import com.trustagro.crm.repository.CrmQuotationRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.hr.entity.LeaveRequestStatus;
import com.trustagro.hr.entity.PayrollRunStatus;
import com.trustagro.hr.repository.LeaveRequestRepository;
import com.trustagro.hr.repository.PayrollRunRepository;
import com.trustagro.inventory.entity.PurchaseOrderStatus;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.procurement.entity.ProcRequisitionStatus;
import com.trustagro.procurement.repository.ProcRequisitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlReportsService {

    public static final String OK = "OK";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private static final long FAILED_LOGIN_WARNING_THRESHOLD = 5;

    private final InvoiceRepository invoiceRepository;
    private final CrmQuotationRepository quotationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProcRequisitionRepository procRequisitionRepository;
    private final AuditService auditService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRunRepository payrollRunRepository;

    public List<ControlCheckResult> runAll() {
        List<ControlCheckResult> results = new ArrayList<>();
        results.add(checkOpenInvoicesWithoutSource());
        results.add(checkAcceptedQuotationsUnconverted());
        results.add(checkNegativeInventory());
        results.add(checkPendingProcurementApprovals());
        results.add(checkFailedLogins24h());
        return results;
    }

    public ControlCheckResult checkOpenInvoicesWithoutSource() {
        try {
            long count = invoiceRepository.countOpenWithoutSource();
            String status = count == 0 ? OK : (count >= 5 ? ERROR : WARNING);
            return ControlCheckResult.builder()
                    .code("INV_SOURCE_MISSING")
                    .title("Open invoices without source linkage")
                    .status(status)
                    .count(count)
                    .message(count == 0
                            ? "All open invoices have sourceModule/sourceId"
                            : count + " open invoice(s) missing sourceModule or sourceId")
                    .build();
        } catch (Exception ex) {
            return unavailable("INV_SOURCE_MISSING", "Open invoices without source linkage", ex);
        }
    }

    public ControlCheckResult checkAcceptedQuotationsUnconverted() {
        try {
            long count = quotationRepository.countAcceptedWithoutConversion();
            return ControlCheckResult.builder()
                    .code("CRM_ACCEPTED_UNCONVERTED")
                    .title("ACCEPTED quotations without conversion")
                    .status(count == 0 ? OK : ERROR)
                    .count(count)
                    .message(count == 0
                            ? "No orphan ACCEPTED quotations"
                            : count + " ACCEPTED quotation(s) lack convertedOrderId and consultancyRequestId")
                    .build();
        } catch (Exception ex) {
            return unavailable("CRM_ACCEPTED_UNCONVERTED", "ACCEPTED quotations without conversion", ex);
        }
    }

    public ControlCheckResult checkNegativeInventory() {
        try {
            long count = inventoryItemRepository.countNegativeStock();
            return ControlCheckResult.builder()
                    .code("INV_NEGATIVE_STOCK")
                    .title("Inventory items with negative quantity")
                    .status(count == 0 ? OK : ERROR)
                    .count(count)
                    .message(count == 0
                            ? "No negative stock quantities"
                            : count + " item(s) with negative currentStock")
                    .build();
        } catch (Exception ex) {
            return unavailable("INV_NEGATIVE_STOCK", "Inventory items with negative quantity", ex);
        }
    }

    public ControlCheckResult checkPendingProcurementApprovals() {
        try {
            long count = countPendingProcurementApprovals();
            return ControlCheckResult.builder()
                    .code("PROC_PENDING_APPROVALS")
                    .title("Pending procurement approvals")
                    .status(count == 0 ? OK : WARNING)
                    .count(count)
                    .message(count == 0
                            ? "No procurement items awaiting approval"
                            : count + " purchase order(s)/requisition(s) awaiting approval")
                    .build();
        } catch (Exception ex) {
            return unavailable("PROC_PENDING_APPROVALS", "Pending procurement approvals", ex);
        }
    }

    public ControlCheckResult checkFailedLogins24h() {
        try {
            long count = auditService.failedLoginsSince(LocalDateTime.now().minusHours(24));
            return ControlCheckResult.builder()
                    .code("AUTH_FAILED_LOGINS_24H")
                    .title("Failed logins (24h)")
                    .status(count == 0 ? OK : WARNING)
                    .count(count)
                    .message(count == 0
                            ? "No failed logins in the last 24 hours"
                            : count + " failed login attempt(s) in the last 24 hours"
                                    + (count >= FAILED_LOGIN_WARNING_THRESHOLD
                                    ? " (above threshold " + FAILED_LOGIN_WARNING_THRESHOLD + ")"
                                    : ""))
                    .build();
        } catch (Exception ex) {
            return unavailable("AUTH_FAILED_LOGINS_24H", "Failed logins (24h)", ex);
        }
    }

    public long countPendingProcurementApprovals() {
        return purchaseOrderRepository.countByStatus(PurchaseOrderStatus.SUBMITTED)
                + procRequisitionRepository.countByStatus(ProcRequisitionStatus.SUBMITTED)
                + procRequisitionRepository.countByStatus(ProcRequisitionStatus.UNDER_REVIEW);
    }

    public long countPendingLeaveApprovals() {
        try {
            return leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING);
        } catch (Exception ex) {
            return 0L;
        }
    }

    public long countPendingPayrollApprovals() {
        try {
            return payrollRunRepository.countByStatus(PayrollRunStatus.CALCULATED);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private ControlCheckResult unavailable(String code, String title, Exception ex) {
        return ControlCheckResult.builder()
                .code(code)
                .title(title)
                .status(UNAVAILABLE)
                .count(0)
                .message("Check unavailable: " + ex.getMessage())
                .build();
    }
}
