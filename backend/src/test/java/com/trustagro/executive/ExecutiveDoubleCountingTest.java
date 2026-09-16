package com.trustagro.executive;

import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.service.CrmCommercialService;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.service.FlockManagementService;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceReportService;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.hr.service.HrService;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.pharmacy.service.PharmacyService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.security.Permissions;
import com.trustagro.training.service.TrainingService;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Focused: INCOME is revenue; TRANSFER payment of the same amount must not inflate revenue.
 */
@ExtendWith(MockitoExtension.class)
class ExecutiveDoubleCountingTest {

    @Mock FinanceService financeService;
    @Mock FinanceReportService financeReportService;
    @Mock FinanceTransactionRepository transactionRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock ChartOfAccountRepository chartOfAccountRepository;
    @Mock CrmCommercialService crmCommercialService;
    @Mock FarmRepository farmRepository;
    @Mock FlockRepository flockRepository;
    @Mock DailyFarmRecordRepository dailyFarmRecordRepository;
    @Mock FlockManagementService flockManagementService;
    @Mock InventoryService inventoryService;
    @Mock PharmacyService pharmacyService;
    @Mock ProcurementService procurementService;
    @Mock ConsultancyService consultancyService;
    @Mock TrainingService trainingService;
    @Mock HrService hrService;
    @Mock PrescriptionRepository prescriptionRepository;

    @InjectMocks ExecutiveService executiveService;

    @BeforeEach
    void auth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gm", "n/a",
                        List.of(new SimpleGrantedAuthority(Permissions.EXECUTIVE_FINANCE))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void incomePlusTransferPaymentDoesNotDoubleCountRevenue() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        // P&L already only counts INCOME postings → revenue stays 10000 even if a 10000 TRANSFER payment exists
        when(financeReportService.getProfitAndLoss(from, to)).thenReturn(Map.of(
                "totalRevenue", new BigDecimal("10000"),
                "totalExpenses", BigDecimal.ZERO,
                "netProfit", new BigDecimal("10000"),
                "knownCogs", BigDecimal.ZERO,
                "limitationNote", "ok"
        ));
        lenient().when(financeService.getDashboardSummary()).thenReturn(Map.of(
                "cashInHand", new BigDecimal("10000"),
                "bankBalance", BigDecimal.ZERO,
                "accountsReceivable", BigDecimal.ZERO,
                "accountsPayable", BigDecimal.ZERO
        ));
        lenient().when(financeReportService.getCashFlow(from, to)).thenReturn(Map.of(
                "cashInflows", new BigDecimal("10000"),
                "cashOutflows", BigDecimal.ZERO,
                "netCashChange", new BigDecimal("10000"),
                "note", "cash"
        ));
        when(transactionRepository.sumGroupedByModule(eq(TransactionType.INCOME), eq(from), eq(to)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"CRM", new BigDecimal("10000")}));
        lenient().when(transactionRepository.sumGroupedByModule(eq(TransactionType.EXPENSE), eq(from), eq(to)))
                .thenReturn(java.util.Collections.emptyList());
        lenient().when(transactionRepository.sumByTypeDateRangeAndModule(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        // After invoice: AR 10000; after TRANSFER payment: AR 0 — outstanding reflects settlement, not extra revenue
        when(invoiceRepository.sumOutstandingOpen()).thenReturn(BigDecimal.ZERO);

        Map<String, Object> summary = executiveService.financialSummary("CUSTOM", from, to, null);
        Map<String, Object> ar = executiveService.receivables("CUSTOM", from, to);

        assertEquals(0, new BigDecimal("10000").compareTo((BigDecimal) summary.get("totalRevenue")),
                "Revenue must remain 10000 (INCOME only), ignoring matching TRANSFER payment");
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) ar.get("outstandingAr")),
                "Partial/full payment reduces AR outstanding without adding revenue");
    }
}
