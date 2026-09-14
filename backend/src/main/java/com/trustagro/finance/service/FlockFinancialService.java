package com.trustagro.finance.service;

import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import com.trustagro.farm.sale.repository.ChickenSaleRepository;
import com.trustagro.finance.entity.FarmCostAllocation;
import com.trustagro.finance.repository.FarmCostAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flock economics from persisted allocations and chicken-sale invoices only.
 * Does not invent profitability when cost or revenue is missing.
 */
@Service
@RequiredArgsConstructor
public class FlockFinancialService {

    private static final Set<ChickenSaleStatus> REVENUE_STATUSES = EnumSet.of(
            ChickenSaleStatus.INVOICED,
            ChickenSaleStatus.PARTIALLY_PAID,
            ChickenSaleStatus.PAID,
            ChickenSaleStatus.COMPLETED
    );

    private final FarmCostAllocationRepository costRepository;
    private final ChickenSaleRepository chickenSaleRepository;
    private final FlockRepository flockRepository;
    private final FlockCOGSService flockCOGSService;

    public Map<String, Object> getFlockProfitLoss(Long flockId) {
        Map<String, Object> report = new HashMap<>();
        report.put("flockId", flockId);

        Flock flock = flockRepository.findById(flockId).orElse(null);
        Integer birds = flock != null ? flock.getCurrentBirdCount() : null;
        if (birds == null && flock != null) {
            birds = flock.getInitialBirdCount();
        }

        Map<String, Object> cogs = flockCOGSService.calculateCOGS(flockId, birds);
        BigDecimal totalCost = (BigDecimal) cogs.getOrDefault("totalCost", BigDecimal.ZERO);
        report.put("totalCost", totalCost);
        report.put("cogs", totalCost);
        report.put("breakdown", cogs.get("breakdown"));
        report.put("costPerBird", cogs.get("costPerBird"));
        report.put("survivingBirds", birds);
        report.put("costKnown", totalCost.compareTo(BigDecimal.ZERO) > 0);

        List<ChickenSale> sales = chickenSaleRepository.findByFlockIdOrderBySaleDateDesc(flockId);
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cashReceived = BigDecimal.ZERO;
        int invoicedSales = 0;
        for (ChickenSale sale : sales) {
            if (!REVENUE_STATUSES.contains(sale.getStatus())) {
                continue;
            }
            invoicedSales++;
            // Recognized revenue = net of discount (subtotal - discount), matching invoice posting
            BigDecimal net = nvl(sale.getSubtotal()).subtract(nvl(sale.getDiscountAmount()));
            if (net.compareTo(BigDecimal.ZERO) < 0) {
                net = BigDecimal.ZERO;
            }
            revenue = revenue.add(net);
            cashReceived = cashReceived.add(nvl(sale.getPaidAmount()));
        }

        boolean revenueKnown = invoicedSales > 0;
        report.put("revenue", revenueKnown ? revenue : null);
        report.put("cashReceived", cashReceived);
        report.put("invoicedSaleCount", invoicedSales);
        report.put("revenueKnown", revenueKnown);

        List<FarmCostAllocation> allocations = costRepository.findByFlockId(flockId);
        report.put("allocationCount", allocations.size());

        if (revenueKnown && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = revenue.subtract(totalCost);
            report.put("profit", profit);
            report.put("netProfit", profit);
            report.put("grossMargin", revenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(revenue, 4, RoundingMode.HALF_UP) : null);
            report.put("profitabilityNote", "Gross result uses known FarmCostAllocation costs only; unallocated costs are not assumed zero.");
        } else {
            report.put("profit", null);
            report.put("netProfit", null);
            report.put("grossMargin", null);
            if (!revenueKnown && totalCost.compareTo(BigDecimal.ZERO) == 0) {
                report.put("profitabilityNote", "No invoiced chicken sales and no cost allocations — profitability unavailable.");
            } else if (!revenueKnown) {
                report.put("profitabilityNote", "Cost known but no invoiced chicken sales — revenue unavailable.");
            } else {
                report.put("profitabilityNote", "Revenue known but flock cost allocations are empty — do not treat missing cost as zero profit.");
            }
        }

        report.put("totalIncome", revenueKnown ? revenue : BigDecimal.ZERO);
        report.put("totalExpense", totalCost);
        report.put("expenseBreakdown", cogs.get("breakdown"));
        return report;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
