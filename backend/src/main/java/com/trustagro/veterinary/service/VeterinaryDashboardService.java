package com.trustagro.veterinary.service;

import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.veterinary.dto.DiseaseCaseResponse;
import com.trustagro.veterinary.entity.DiseaseStatus;
import com.trustagro.veterinary.entity.HealthIssueReportStatus;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.entity.TreatmentStatus;
import com.trustagro.veterinary.entity.VaccinationStatus;
import com.trustagro.veterinary.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VeterinaryDashboardService {

    private final DiseaseCaseRepository diseaseCaseRepo;
    private final VaccinationScheduleRepository vaccinationRepo;
    private final TreatmentRecordRepository treatmentRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final HealthIssueReportRepository healthIssueReportRepo;
    private final DailyFarmRecordRepository dailyRecordRepo;
    private final InventoryItemRepository inventoryItemRepo;
    private final StockBatchRepository stockBatchRepo;
    private final VeterinaryService veterinaryService;

    public Map<String, Object> getDashboardData() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        Map<String, Object> data = new HashMap<>();
        data.put("activeDiseaseCases", diseaseCaseRepo.countByStatus(DiseaseStatus.ACTIVE));
        data.put("vaccinationsDueThisWeek", vaccinationRepo
                .findByScheduledDateBetweenAndStatus(today, weekEnd, VaccinationStatus.SCHEDULED).size());
        data.put("missedVaccinations", vaccinationRepo
                .findByScheduledDateBeforeAndStatus(today, VaccinationStatus.SCHEDULED).size());
        data.put("upcomingVaccinations", vaccinationRepo
                .findByScheduledDateAfterAndStatus(today, VaccinationStatus.SCHEDULED).size());
        data.put("activeTreatments", treatmentRepo.countByStatus(TreatmentStatus.ACTIVE));
        data.put("openHealthReports", healthIssueReportRepo.countByStatus(HealthIssueReportStatus.OPEN));
        data.put("pendingPrescriptions",
                prescriptionRepo.countByStatus(PrescriptionStatus.PENDING_PHARMACY)
                        + prescriptionRepo.countByStatus(PrescriptionStatus.PENDING)
                        + prescriptionRepo.countByStatus(PrescriptionStatus.APPROVED));

        int mortality7d = dailyRecordRepo.sumMortalityBetween(today.minusDays(7), today);
        int openingEstimate = Math.max(mortality7d * 20, 1);
        data.put("mortalityRate7Day", Math.round((mortality7d * 10000.0 / openingEstimate)) / 100.0);
        data.put("drugStockAlerts", countLowDrugStock());

        data.put("mortalityTrend", buildMortalityTrend(thirtyDaysAgo, today));
        data.put("diseaseBreakdown", buildDiseaseBreakdown());
        data.put("recentDiseaseCases", diseaseCaseRepo.findByStatusOrderByDateDetectedDesc(DiseaseStatus.ACTIVE)
                .stream().limit(10).map(veterinaryService::toDiseaseCaseResponse).collect(Collectors.toList()));
        data.put("recentTreatments", veterinaryService.getRecentTreatments(5));
        data.put("alerts", buildAlerts(today));

        return data;
    }

    private int countLowDrugStock() {
        try {
            return (int) inventoryItemRepo.findAll().stream()
                    .filter(item -> item.getMinimumStockLevel() != null
                            && stockBatchRepo.getTotalStock(item.getId()) < item.getMinimumStockLevel())
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, Object>> buildMortalityTrend(LocalDate start, LocalDate end) {
        List<Object[]> rows = dailyRecordRepo.sumMortalityGroupedByDate(start, end);
        return rows.stream().map(row -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", row[0].toString());
            point.put("mortality", ((Number) row[1]).intValue());
            return point;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDiseaseBreakdown() {
        return diseaseCaseRepo.findByStatusOrderByDateDetectedDesc(DiseaseStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(
                        dc -> dc.getSuspectedDisease() != null && !dc.getSuspectedDisease().isBlank()
                                ? dc.getSuspectedDisease() : "Unspecified",
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(8)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAlerts(LocalDate today) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        long missed = vaccinationRepo.findByScheduledDateBeforeAndStatus(today, VaccinationStatus.SCHEDULED).size();
        if (missed > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "critical");
            alert.put("title", "Missed Vaccinations");
            alert.put("description", missed + " vaccination(s) are overdue.");
            alerts.add(alert);
        }
        int lowStock = countLowDrugStock();
        if (lowStock > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "warning");
            alert.put("title", "Drug Stock Alerts");
            alert.put("description", lowStock + " inventory item(s) below minimum stock.");
            alerts.add(alert);
        }
        return alerts;
    }
}
