package com.trustagro.finance.service;

import com.trustagro.finance.entity.FarmCostAllocation;
import com.trustagro.finance.repository.FarmCostAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FlockCOGSService {

    private final FarmCostAllocationRepository costRepository;

    public Map<String, Object> calculateCOGS(Long flockId, Integer survivingChickens) {
        List<FarmCostAllocation> costs = costRepository.findByFlockId(flockId);
        BigDecimal totalCost = costs.stream()
                .map(FarmCostAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalCost", totalCost);
        report.put("survivingChickens", survivingChickens);
        
        if (survivingChickens != null && survivingChickens > 0) {
            report.put("costPerBird", totalCost.divide(BigDecimal.valueOf(survivingChickens), 2, RoundingMode.HALF_UP));
        }

        // Categorized costs
        Map<String, BigDecimal> breakdown = new HashMap<>();
        for (FarmCostAllocation cost : costs) {
            String type = cost.getCostType().name();
            breakdown.put(type, breakdown.getOrDefault(type, BigDecimal.ZERO).add(cost.getAmount()));
        }
        report.put("breakdown", breakdown);
        
        return report;
    }
}
