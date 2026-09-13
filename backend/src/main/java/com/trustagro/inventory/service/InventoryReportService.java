package com.trustagro.inventory.service;

import com.trustagro.inventory.dto.InventoryItemResponse;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class InventoryReportService {

    private final InventoryItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final com.trustagro.inventory.repository.StockMovementRepository movementRepository;
    private final InventoryService inventoryService;

    public InventoryReportService(InventoryItemRepository itemRepository,
                                  StockBatchRepository batchRepository,
                                  com.trustagro.inventory.repository.StockMovementRepository movementRepository,
                                  InventoryService inventoryService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.inventoryService = inventoryService;
    }

    public Map<String, Object> getStockValuationReport() {
        List<com.trustagro.inventory.dto.InventoryItemResponse> items = inventoryService.getAllItems();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        List<Map<String, Object>> itemValuations = items.stream().map(item -> {
            List<StockBatch> batches = batchRepository.findByItemIdOrderByReceivedDateAsc(item.getId());
            BigDecimal itemValue = batches.stream()
                    .map(b -> (b.getUnitCost() != null ? b.getUnitCost() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(b.getQuantityRemaining() != null ? b.getQuantityRemaining() : 0.0)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> map = new HashMap<>();
            map.put("itemId", item.getId());
            map.put("itemName", item.getItemName());
            map.put("sku", item.getSku());
            map.put("category", item.getCategory());
            map.put("currentStock", item.getCurrentStock());
            map.put("valuation", itemValue);
            return map;
        }).collect(Collectors.toList());

        totalValue = itemValuations.stream()
                .map(m -> (BigDecimal) m.get("valuation"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("totalValue", totalValue);
        report.put("items", itemValuations);
        report.put("generatedAt", java.time.LocalDateTime.now());
        return report;
    }

    public String generateValuationCsv() {
        Map<String, Object> report = getStockValuationReport();
        List<Map<String, Object>> items = (List<Map<String, Object>>) report.get("items");
        
        StringBuilder sb = new StringBuilder();
        sb.append("SKU,Item Name,Category,Current Stock,Valuation\n");
        for (Map<String, Object> item : items) {
            sb.append(item.get("sku")).append(",")
              .append(item.get("itemName")).append(",")
              .append(item.get("category")).append(",")
              .append(item.get("currentStock")).append(",")
              .append(item.get("valuation")).append("\n");
        }
        return sb.toString();
    }

    public String generateExpiryCsv() {
        List<StockBatch> batches = inventoryService.getExpiringItems();
        StringBuilder sb = new StringBuilder();
        sb.append("Batch Number,Item Name,Expiry Date,Quantity Remaining\n");
        for (StockBatch b : batches) {
            sb.append(b.getBatchNumber()).append(",")
              .append(b.getItem().getItemName()).append(",")
              .append(b.getExpiryDate()).append(",")
              .append(b.getQuantityRemaining()).append("\n");
        }
        return sb.toString();
    }

    public List<Map<String, Object>> getTurnoverReport() {
        // Simple turnover calculation: (Cost of Goods Sold / Average Inventory)
        // For the UI chart, we'll return monthly sales movement totals for the last 6 months
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate now = LocalDate.now();
        
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDate start = month.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate end = month.with(TemporalAdjusters.lastDayOfMonth());
            
            // This is a proxy for turnover: total quantity issued/sold in that month
            double totalOut = movementRepository.findAll().stream()
                .filter(m -> m.getMovementType() != com.trustagro.inventory.entity.MovementType.RECEIPT)
                .filter(m -> !m.getCreatedAt().toLocalDate().isBefore(start) && !m.getCreatedAt().toLocalDate().isAfter(end))
                .mapToDouble(com.trustagro.inventory.entity.StockMovement::getQuantity)
                .sum();
                
            Map<String, Object> point = new HashMap<>();
            point.put("month", month.getMonth().name().substring(0, 3));
            point.put("turnover_rate", totalOut);
            data.add(point);
        }
        return data;
    }

    public Map<String, Object> getInventorySummary() {
        Map<String, Object> stats = inventoryService.getDashboardStats();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalValue", stats.getOrDefault("total_value", 0));
        summary.put("totalSkus", stats.getOrDefault("total_skus", 0));
        summary.put("lowStockCount", stats.getOrDefault("low_stock_count", 0));
        summary.put("expiringCount", stats.getOrDefault("expiring_count", 0));
        summary.put("inventoryTurnover", stats.getOrDefault("inventory_turnover", 0));
        return summary;
    }
}
