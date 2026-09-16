package com.trustagro.executive;

import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.service.CrmCommercialService;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.service.FlockManagementService;
import com.trustagro.finance.entity.FinanceSourceTypes;
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
import com.trustagro.veterinary.entity.PrescriptionStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutiveServiceTest {

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
    void setUp() {
        ReflectionTestUtils.setField(executiveService, "mortalityAlertThreshold", 0.0);
        lenientDefaults();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void lenientDefaults() {
        lenient().when(financeReportService.getProfitAndLoss(any(), any())).thenReturn(Map.of(
                "totalRevenue", new BigDecimal("10000"),
                "totalExpenses", new BigDecimal("3000"),
                "netProfit", new BigDecimal("7000"),
                "knownCogs", new BigDecimal("500"),
                "limitationNote", "note"
        ));
        lenient().when(financeService.getDashboardSummary()).thenReturn(Map.of(
                "cashInHand", new BigDecimal("1000"),
                "bankBalance", new BigDecimal("2000"),
                "accountsReceivable", new BigDecimal("1500"),
                "accountsPayable", new BigDecimal("800")
        ));
        lenient().when(financeReportService.getCashFlow(any(), any())).thenReturn(Map.of(
                "cashInflows", new BigDecimal("4000"),
                "cashOutflows", new BigDecimal("1000"),
                "netCashChange", new BigDecimal("3000"),
                "note", "cash note"
        ));
        lenient().when(transactionRepository.sumGroupedByModule(eq(TransactionType.INCOME), any(), any()))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"CRM", new BigDecimal("10000")}));
        lenient().when(transactionRepository.sumGroupedByModule(eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"HR", new BigDecimal("3000")}));
        lenient().when(transactionRepository.sumByTypeDateRangeAndModule(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(invoiceRepository.sumOutstandingOpen()).thenReturn(new BigDecimal("1500"));
        lenient().when(invoiceRepository.countOpen()).thenReturn(3L);
        lenient().when(invoiceRepository.countOverdueOpen(any())).thenReturn(0L);
        lenient().when(invoiceRepository.sumOverdueOutstanding(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(crmCommercialService.commercialDashboard()).thenReturn(Map.of(
                "conversionRate", new BigDecimal("25.00"),
                "totalOrderRevenueInvoiced", new BigDecimal("5000"),
                "totalOrderPaid", new BigDecimal("3500"),
                "outstandingAr", new BigDecimal("1500"),
                "pipelineLeads", Map.of(),
                "opportunitiesByStage", Map.of(),
                "openOpportunityValue", BigDecimal.ZERO,
                "quotationsByStatus", Map.of(),
                "ordersByStatus", Map.of(),
                "openTasks", 0L
        ));
        lenient().when(farmRepository.countByStatus(FarmStatus.ACTIVE)).thenReturn(2L);
        lenient().when(flockRepository.countByStatus(FlockStatus.ACTIVE)).thenReturn(5L);
        lenient().when(dailyFarmRecordRepository.sumMortalityBetween(any(), any())).thenReturn(0);
        lenient().when(dailyFarmRecordRepository.sumMortalityByDate(any())).thenReturn(0);
        lenient().when(flockManagementService.calculateFCR(any())).thenReturn(null);
        lenient().when(inventoryService.getDashboardStats()).thenReturn(Map.of(
                "total_value", 100.0, "low_stock_count", 0, "expiring_count", 0, "total_skus", 10
        ));
        lenient().when(pharmacyService.getDashboardStats()).thenReturn(Map.of(
                "revenue_today", BigDecimal.ZERO, "sales_count_today", 0
        ));
        lenient().when(procurementService.dashboard()).thenReturn(Map.of(
                "openRequisitions", 0L, "openPurchaseOrders", 0L,
                "approvedUnpaidInvoices", 0L, "apOutstanding", BigDecimal.ZERO,
                "invoicesAwaitingMatch", 0L
        ));
        lenient().when(consultancyService.dashboard()).thenReturn(Map.of(
                "activeEngagements", 1L, "openRequests", 0L,
                "revenue", new BigDecimal("100"), "outstandingAmount", BigDecimal.ZERO
        ));
        lenient().when(trainingService.dashboard()).thenReturn(Map.of(
                "activePrograms", 1L, "upcomingSessionCount", 0,
                "revenue", new BigDecimal("50"), "outstandingAmount", BigDecimal.ZERO
        ));
        lenient().when(hrService.dashboard()).thenReturn(Map.of(
                "totalEmployees", 10L, "activeHeadcount", 9L, "onLeave", 1L
        ));
        lenient().when(prescriptionRepository.countByStatus(any())).thenReturn(0L);
        lenient().when(chartOfAccountRepository.findByAccountCode(any())).thenReturn(java.util.Optional.empty());
    }

    private void authWith(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("exec", "n/a",
                        java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void financialKpisComeFromFinanceReportServiceIncome() {
        authWith(Permissions.EXECUTIVE_FINANCE);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 15);

        Map<String, Object> summary = executiveService.financialSummary("CUSTOM", from, to, null);

        assertEquals(0, new BigDecimal("10000").compareTo((BigDecimal) summary.get("totalRevenue")));
        assertEquals(0, new BigDecimal("3000").compareTo((BigDecimal) summary.get("totalExpenses")));
        assertEquals(0, new BigDecimal("7000").compareTo((BigDecimal) summary.get("netProfit")));
        verify(financeReportService).getProfitAndLoss(from, to);
        verify(transactionRepository).sumGroupedByModule(TransactionType.INCOME, from, to);
    }

    @Test
    void dateRangeCallsRepositoryWithInclusiveBoundsAndExcludesPreviousMonth() {
        authWith(Permissions.EXECUTIVE_FINANCE);
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);

        executiveService.financialSummary("CUSTOM", from, to, null);

        verify(financeReportService).getProfitAndLoss(from, to);
        verify(transactionRepository, never()).sumGroupedByModule(eq(TransactionType.INCOME),
                eq(LocalDate.of(2026, 8, 1)), any());
        verify(transactionRepository).sumGroupedByModule(TransactionType.INCOME, from, to);
    }

    @Test
    void businessLineFilterUsesModuleScopedIncomeSum() {
        authWith(Permissions.EXECUTIVE_FINANCE);
        when(transactionRepository.sumByTypeDateRangeAndModule(
                eq(TransactionType.INCOME), any(), any(), eq("CRM")))
                .thenReturn(new BigDecimal("2500"));
        when(transactionRepository.sumByTypeDateRangeAndModule(
                eq(TransactionType.EXPENSE), any(), any(), eq("CRM")))
                .thenReturn(new BigDecimal("100"));

        Map<String, Object> summary = executiveService.financialSummary("THIS_MONTH", null, null, "CRM");

        assertEquals(0, new BigDecimal("2500").compareTo((BigDecimal) summary.get("totalRevenue")));
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) summary.get("totalExpenses")));
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byMod = (Map<String, BigDecimal>) summary.get("revenueByModule");
        assertTrue(byMod.containsKey("CRM"));
        assertEquals(1, byMod.size());
    }

    @Test
    void workforceOmittedWithoutExecutiveHr() {
        authWith(Permissions.EXECUTIVE_DASHBOARD, Permissions.EXECUTIVE_SALES, Permissions.EXECUTIVE_FARM,
                Permissions.EXECUTIVE_INVENTORY, Permissions.EXECUTIVE_REPORT);
        Map<String, Object> dash = executiveService.dashboard("THIS_MONTH", null, null, null);
        assertFalse(dash.containsKey("workforce"));
        verify(hrService, never()).dashboard();
    }

    @Test
    void workforceIncludedWithExecutiveHr() {
        authWith(Permissions.EXECUTIVE_DASHBOARD, Permissions.EXECUTIVE_HR);
        Map<String, Object> dash = executiveService.dashboard("THIS_MONTH", null, null, null);
        assertTrue(dash.containsKey("workforce"));
        assertEquals("AVAILABLE", ((Map<?, ?>) dash.get("workforce")).get("status"));
    }

    @Test
    void incompleteFcrMarkedUnavailable() {
        authWith(Permissions.EXECUTIVE_FARM);
        when(flockManagementService.calculateFCR(any())).thenReturn(null);
        Map<String, Object> farm = executiveService.farm("THIS_MONTH", null, null);
        @SuppressWarnings("unchecked")
        Map<String, Object> fcr = (Map<String, Object>) farm.get("fcr");
        assertEquals("UNAVAILABLE", fcr.get("status"));
        assertNull(fcr.get("value"));
    }

    @Test
    void receivablesReflectPartialPaymentOutstanding() {
        authWith(Permissions.EXECUTIVE_FINANCE);
        when(invoiceRepository.sumOutstandingOpen()).thenReturn(new BigDecimal("4000"));
        when(invoiceRepository.countOpen()).thenReturn(2L);

        Map<String, Object> ar = executiveService.receivables("THIS_MONTH", null, null);

        assertEquals(0, new BigDecimal("4000").compareTo((BigDecimal) ar.get("outstandingAr")));
        assertEquals(2L, ar.get("openInvoiceCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> aging = (Map<String, Object>) ar.get("agingBuckets");
        assertEquals("UNAVAILABLE", aging.get("status"));
    }

    @Test
    void alertsOnlyWhenConditionsTrue() {
        authWith(Permissions.EXECUTIVE_DASHBOARD);
        when(invoiceRepository.sumOverdueOutstanding(any())).thenReturn(BigDecimal.ZERO);
        when(inventoryService.getDashboardStats()).thenReturn(Map.of(
                "total_value", 10.0, "low_stock_count", 0, "expiring_count", 0, "total_skus", 1
        ));
        when(dailyFarmRecordRepository.sumMortalityByDate(any())).thenReturn(0);
        when(prescriptionRepository.countByStatus(any())).thenReturn(0L);

        Map<String, Object> empty = executiveService.alerts("THIS_MONTH", null, null);
        assertEquals(0, empty.get("count"));

        when(invoiceRepository.sumOverdueOutstanding(any())).thenReturn(new BigDecimal("500"));
        when(inventoryService.getDashboardStats()).thenReturn(Map.of(
                "total_value", 10.0, "low_stock_count", 3, "expiring_count", 2, "total_skus", 1
        ));
        Map<String, Object> withAlerts = executiveService.alerts("THIS_MONTH", null, null);
        assertTrue((Integer) withAlerts.get("count") >= 3);
    }

    @Test
    void salesNeverInventConversionRate() {
        authWith(Permissions.EXECUTIVE_SALES);
        when(crmCommercialService.commercialDashboard()).thenReturn(Map.of(
                "conversionRate", new BigDecimal("12.50"),
                "totalOrderRevenueInvoiced", new BigDecimal("100"),
                "totalOrderPaid", new BigDecimal("40"),
                "outstandingAr", new BigDecimal("60"),
                "pipelineLeads", Map.of("NEW", 2L),
                "opportunitiesByStage", Map.of(),
                "openOpportunityValue", BigDecimal.TEN,
                "quotationsByStatus", Map.of(),
                "ordersByStatus", Map.of(),
                "openTasks", 1L
        ));
        Map<String, Object> sales = executiveService.sales("THIS_MONTH", null, null, null);
        assertEquals(0, new BigDecimal("12.50").compareTo((BigDecimal) sales.get("conversionRate")));
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) sales.get("crmInvoiced")));
    }
}
