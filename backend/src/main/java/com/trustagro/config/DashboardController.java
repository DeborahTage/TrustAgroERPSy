package com.trustagro.config;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.FinanceReportService;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.notification.repository.NotificationRepository;


import com.trustagro.pharmacy.repository.PharmacySaleRepository;
import com.trustagro.veterinary.entity.HealthIssueReportStatus;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.entity.VaccinationStatus;
import com.trustagro.veterinary.repository.VaccinationScheduleRepository;
import com.trustagro.veterinary.repository.DiseaseCaseRepository;
import com.trustagro.veterinary.repository.HealthIssueReportRepository;
import com.trustagro.veterinary.repository.PrescriptionRepository;

import com.trustagro.crm.service.CrmService;
import com.trustagro.marketing.service.MarketingService;
import com.trustagro.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public DashboardController(FarmRepository farmRepository, FlockRepository flockRepository,
            DailyFarmRecordRepository dailyRecordRepository, InventoryItemRepository inventoryItemRepository,
            StockBatchRepository stockBatchRepository, com.trustagro.pharmacy.repository.PharmacySaleRepository saleRepository,
            FinanceService financeService, FinanceReportService reportService, NotificationRepository notificationRepository,
            VaccinationScheduleRepository vaccinationRepository, DiseaseCaseRepository diseaseCaseRepository,
            HealthIssueReportRepository healthIssueReportRepository, PrescriptionRepository prescriptionRepository,
            UserRepository userRepository, com.trustagro.crm.repository.CrmClientRepository crmClientRepository,
            CrmService crmService, MarketingService marketingService) {
        this.farmRepository = farmRepository;
        this.flockRepository = flockRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.saleRepository = saleRepository;
        this.financeService = financeService;
        this.reportService = reportService;
        this.notificationRepository = notificationRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.diseaseCaseRepository = diseaseCaseRepository;
        this.healthIssueReportRepository = healthIssueReportRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.crmClientRepository = crmClientRepository;
        this.crmService = crmService;
        this.marketingService = marketingService;
    }

    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;
    private final DailyFarmRecordRepository dailyRecordRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final PharmacySaleRepository saleRepository;

    private final FinanceService financeService;
    private final FinanceReportService reportService;
    private final NotificationRepository notificationRepository;
    private final VaccinationScheduleRepository vaccinationRepository;
    private final DiseaseCaseRepository diseaseCaseRepository;
    private final HealthIssueReportRepository healthIssueReportRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final com.trustagro.crm.repository.CrmClientRepository crmClientRepository;
    private final CrmService crmService;
    private final MarketingService marketingService;

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalFarms", farmRepository.countByStatus(FarmStatus.ACTIVE));
        data.put("totalActiveFlocks", flockRepository.countByStatus(FlockStatus.ACTIVE));
        data.put("todayMortality", dailyRecordRepository.sumMortalityByDate(LocalDate.now()));
        LocalDate ytdStart = LocalDate.now().withDayOfYear(1);
        LocalDate today = LocalDate.now();
        var pl = reportService.getProfitAndLoss(ytdStart, today);
        data.put("totalRevenue", pl.get("totalRevenue"));
        data.put("totalExpenses", pl.get("totalExpenses"));
        data.put("netProfitLoss", pl.get("netProfit"));
        data.put("pendingAlerts", notificationRepository.count());
        data.put("activeDiseaseCases", diseaseCaseRepository.count());
        data.put("openHealthReports", healthIssueReportRepository.countByStatus(HealthIssueReportStatus.OPEN));
        data.put("pendingPrescriptions", prescriptionRepository.countByStatus(PrescriptionStatus.PENDING));

        Map<String, Object> financeSummary = financeService.getDashboardSummary();
        data.put("cashInHand", financeSummary.get("cashInHand"));
        data.put("accountsReceivable", financeSummary.get("accountsReceivable"));
        data.put("accountsPayable", financeSummary.get("accountsPayable"));
        data.put("inventoryValue", inventoryItemRepository.findAll().stream()
                .mapToDouble(i -> (i.getCurrentStock() != null ? i.getCurrentStock() : 0.0)
                        * (i.getAvgUnitCost() != null ? i.getAvgUnitCost().doubleValue() : 0.0))
                .sum());
        data.put("lowStockCount", inventoryItemRepository.findAll().stream()
                .filter(i -> i.getReorderPoint() != null && i.getCurrentStock() != null
                        && i.getCurrentStock() <= i.getReorderPoint())
                .count());
        data.put("expiringItems", stockBatchRepository
                .findExpiringBatches(LocalDate.now(), LocalDate.now().plusDays(30)).size());
        data.put("totalUsers", userRepository.count());
        data.put("activeClients", crmClientRepository.count());
        data.put("inventoryAlerts", data.get("lowStockCount"));
        data.put("totalSalesCount", saleRepository.count());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        data.put("pharmacySalesToday", saleRepository.findSalesBetweenDates(startOfDay, LocalDateTime.now()).stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/farm-manager")
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> farmManagerDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalFarms", farmRepository.countByStatus(FarmStatus.ACTIVE));
        data.put("activeFlocks", flockRepository.countByStatus(FlockStatus.ACTIVE));
        data.put("todayMortality", dailyRecordRepository.sumMortalityByDate(LocalDate.now()));
        data.put("upcomingVaccinations", vaccinationRepository
                .findByScheduledDateAfterAndStatus(LocalDate.now(), VaccinationStatus.SCHEDULED).size());
        data.put("openHealthReports", healthIssueReportRepository.countByStatus(HealthIssueReportStatus.OPEN));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/store")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> storeDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalItems", inventoryItemRepository.count());
        data.put("expiringItems", stockBatchRepository
                .findExpiringBatches(LocalDate.now(), LocalDate.now().plusDays(30)).size());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/vet")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> vetDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("upcomingVaccinations", vaccinationRepository
                .findByScheduledDateAfterAndStatus(LocalDate.now(), VaccinationStatus.SCHEDULED).size());
        data.put("missedVaccinations", vaccinationRepository
                .findByScheduledDateBeforeAndStatus(LocalDate.now(), VaccinationStatus.SCHEDULED).size());
        data.put("activeDiseaseCases", diseaseCaseRepository.count());
        data.put("openHealthReports", healthIssueReportRepository.countByStatus(HealthIssueReportStatus.OPEN));
        data.put("pendingPrescriptions", prescriptionRepository.countByStatus(PrescriptionStatus.PENDING));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/pharmacy")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pharmacyDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalSalesCount", saleRepository.count());
        data.put("pendingPrescriptions", prescriptionRepository.countByStatus(PrescriptionStatus.PENDING));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> financeDashboard() {
        LocalDate ytdStart = LocalDate.now().withDayOfYear(1);
        LocalDate today = LocalDate.now();
        var pl = reportService.getProfitAndLoss(ytdStart, today);
        Map<String, Object> data = new HashMap<>();
        data.put("totalIncome", pl.get("totalRevenue"));
        data.put("totalExpenses", pl.get("totalExpenses"));
        data.put("netProfitLoss", pl.get("netProfit"));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/crm")
    @PreAuthorize("hasAnyRole('ADMIN','CRM_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crmDashboard() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getDashboardStats()));
    }

    @GetMapping("/marketing")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> marketingDashboard() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getDashboard()));
    }
}
