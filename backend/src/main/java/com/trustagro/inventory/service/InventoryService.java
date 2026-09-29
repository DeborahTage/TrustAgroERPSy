package com.trustagro.inventory.service;

import com.trustagro.finance.service.FinanceService;
import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.inventory.dto.*;
import com.trustagro.inventory.entity.*;
import com.trustagro.inventory.repository.*;
import com.trustagro.notification.entity.Notification;
import com.trustagro.notification.entity.NotificationType;
import com.trustagro.notification.repository.NotificationRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.trustagro.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final Set<ItemCategory> PHARMACY_POS_CATEGORIES = EnumSet.of(
            ItemCategory.DRUG,
            ItemCategory.VACCINE,
            ItemCategory.VITAMIN,
            ItemCategory.EQUIPMENT,
            ItemCategory.FEED,
            ItemCategory.FARM_MATERIAL
    );

    private final InventoryItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final DemandForecastRepository forecastRepository;
    private final NotificationRepository notificationRepository;
    private final FinanceService financeService;
    private final UserRepository userRepository;
    private final InternalRequisitionRepository requisitionRepository;
    private final AuditService auditService;

    public InventoryService(InventoryItemRepository itemRepository, 
                            StockBatchRepository batchRepository,
                            StockMovementRepository movementRepository,
                            PurchaseOrderRepository poRepository,
                            PurchaseOrderItemRepository poItemRepository,
                            DemandForecastRepository forecastRepository,
                            NotificationRepository notificationRepository,
                            FinanceService financeService,
                            UserRepository userRepository,
                            InternalRequisitionRepository requisitionRepository,
                            AuditService auditService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.poRepository = poRepository;
        this.poItemRepository = poItemRepository;
        this.forecastRepository = forecastRepository;
        this.notificationRepository = notificationRepository;
        this.financeService = financeService;
        this.userRepository = userRepository;
        this.requisitionRepository = requisitionRepository;
        this.auditService = auditService;
    }


    // ============================================================
    // BARCODE / RFID SCANNING
    // ============================================================

    public Map<String, Object> scanItem(String barcode, User user) {
        InventoryItem item = itemRepository.findByBarcodeOrRfidTag(barcode, barcode)
                .orElseThrow(() -> new IllegalArgumentException("Item not found for barcode: " + barcode));

        List<StockBatch> batches = batchRepository.findAvailableBatchesFEFO(item.getId(), LocalDate.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", item);
        result.put("batches", batches);
        result.put("scannedAt", LocalDateTime.now());
        result.put("scannedBy", user != null ? user.getFullName() : "System");
        return result;
    }

    @Transactional
    public Map<String, Object> processScanAction(Map<String, Object> scanData, User user) {
        String action = (String) scanData.get("action");
        String barcode = (String) scanData.get("barcode");
        Double quantity = Double.valueOf(scanData.get("quantity").toString());

        Map<String, Object> scanned = scanItem(barcode, user);
        InventoryItem item = (InventoryItem) scanned.get("item");

        return switch (action) {
            case "RECEIVE" -> stockIn(item.getId(), quantity, scanData, user);
            case "ISSUE" -> stockOut(item.getId(), quantity, "ISSUE", null, user);
            case "COUNT" -> recordCount(item.getId(), quantity, user);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    // ============================================================
    // STOCK IN (RECEIPT)
    // ============================================================

    @Transactional
    public Map<String, Object> stockIn(Long itemId, Double quantity, Map<String, Object> data, User user) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));

        StockBatch batch = new StockBatch();
        batch.setItem(item);
        Object batchObj = data.get("batchNumber");
        String batchNum = (batchObj != null && !batchObj.toString().trim().isEmpty())
                ? batchObj.toString()
                : "BATCH-" + System.currentTimeMillis();
        batch.setBatchNumber(batchNum);
        batch.setQuantityReceived(quantity);
        batch.setQuantityRemaining(quantity);
        
        Object unitCostObj = data.get("unitCost");
        batch.setUnitCost((unitCostObj != null && !unitCostObj.toString().trim().isEmpty()) 
                ? new BigDecimal(unitCostObj.toString()) : null);
                
        Object expiryObj = data.get("expiryDate");
        if (expiryObj != null && !expiryObj.toString().trim().isEmpty()) {
            batch.setExpiryDate(LocalDate.parse(expiryObj.toString()));
        }
        batch.setReceivedDate(LocalDate.now());
        batch.setReceivedBy(user);
        batch.setStatus(BatchStatus.AVAILABLE);
        batch.setStorageZone(data.getOrDefault("storageZone", "").toString());
        batch.setShelfLocation(data.getOrDefault("shelfLocation", "").toString());
        Object poRefObj = data.get("poReference");
        if (poRefObj != null && !poRefObj.toString().trim().isEmpty()) {
            batch.setPoReference(poRefObj.toString().trim());
        }
        Object mfgObj = data.get("manufactureDate");
        if (mfgObj != null && !mfgObj.toString().trim().isEmpty()) {
            batch.setManufacturingDate(LocalDate.parse(mfgObj.toString()));
        }
        // StockBatch has no supplier FK — supplierId in data is ignored here (GRN owns the link).
        batchRepository.save(batch);

        // Update item stock and weighted avg cost
        double newStock = (item.getCurrentStock() != null ? item.getCurrentStock() : 0.0) + quantity;
        if (batch.getUnitCost() != null) {
            double oldValue = (item.getCurrentStock() != null ? item.getCurrentStock() : 0.0)
                    * (item.getAvgUnitCost() != null ? item.getAvgUnitCost().doubleValue() : 0.0);
            double newValue = quantity * batch.getUnitCost().doubleValue();
            item.setAvgUnitCost(BigDecimal.valueOf((oldValue + newValue) / newStock));
            item.setLastPurchasePrice(batch.getUnitCost());
        }
        item.setCurrentStock(newStock);
        itemRepository.save(item);

        // Log movement
        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setBatch(batch);
        movement.setMovementType(MovementType.RECEIPT);
        movement.setQuantity(quantity);
        movement.setUnitCost(batch.getUnitCost());
        movement.setTotalCost(batch.getUnitCost() != null ? batch.getUnitCost().multiply(BigDecimal.valueOf(quantity)) : null);
        movement.setToLocation(batch.getStorageZone() + "-" + batch.getShelfLocation());
        movement.setCreatedBy(user);
        Object refTypeObj = data.get("referenceType");
        Object refIdObj = data.get("referenceId");
        if (refIdObj == null && data.get("grnId") != null) {
            refIdObj = data.get("grnId");
            if (refTypeObj == null) {
                refTypeObj = com.trustagro.finance.entity.FinanceSourceTypes.REF_PROCUREMENT_GRN;
            }
        }
        if (refTypeObj != null && !refTypeObj.toString().trim().isEmpty()) {
            movement.setReferenceType(refTypeObj.toString().trim());
        }
        if (refIdObj != null && !refIdObj.toString().trim().isEmpty()) {
            movement.setReferenceId(Long.valueOf(refIdObj.toString()));
        }
        movementRepository.save(movement);

        // Auto-create inventory receipt on AP (asset, not expense). Failures are fatal for integrity.
        if (batch.getUnitCost() != null && batch.getUnitCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
            financeService.postInventoryReceipt(
                    batch.getUnitCost().multiply(java.math.BigDecimal.valueOf(quantity)),
                    "Stock receipt: " + item.getItemName() + " x" + quantity,
                    com.trustagro.finance.entity.FinanceSourceTypes.MODULE_INVENTORY,
                    com.trustagro.finance.entity.FinanceSourceTypes.REF_STOCK_IN,
                    batch.getId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batch.getId());
        result.put("newStock", newStock);
        auditService.log("STOCK_IN", "INVENTORY", "InventoryItem", itemId,
                null, "qty=" + quantity + ",newStock=" + newStock + ",batch=" + batch.getId());
        return result;
    }

    // ============================================================
    // STOCK OUT (FEFO / FIFO)
    // ============================================================

    @Transactional
    public Map<String, Object> stockOut(Long itemId, Double quantity, String reason, Long referenceId, User user) {
        return stockOut(itemId, quantity, reason, referenceId, user, null, null, null);
    }

    @Transactional
    public Map<String, Object> stockOut(Long itemId, Double quantity, String reason, Long referenceId, User user,
                                        Long batchId, String referenceType, String movementTypeOverride) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));

        List<StockBatch> batches;
        if (batchId != null) {
            StockBatch selected = batchRepository.findById(batchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
            if (selected.getItem() == null || !itemId.equals(selected.getItem().getId())) {
                throw new IllegalStateException("Selected batch does not belong to inventory item");
            }
            if (selected.getQuantityRemaining() == null || selected.getQuantityRemaining() <= 0) {
                throw new IllegalStateException("Selected batch has no remaining quantity");
            }
            batches = List.of(selected);
        } else {
            batches = batchRepository.findAvailableBatchesFEFO(itemId, LocalDate.now());
        }

        double remaining = quantity;
        List<StockMovement> movements = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;

        MovementType movementType = resolveMovementType(reason, movementTypeOverride);

        for (StockBatch batch : batches) {
            if (remaining <= 0) break;
            double deduct = Math.min(remaining, batch.getQuantityRemaining());
            remaining -= deduct;
            batch.setQuantityRemaining(batch.getQuantityRemaining() - deduct);
            batchRepository.save(batch);

            StockMovement mv = new StockMovement();
            mv.setItem(item);
            mv.setBatch(batch);
            mv.setMovementType(movementType);
            mv.setQuantity(deduct);
            mv.setUnitCost(batch.getUnitCost());
            BigDecimal lineCost = batch.getUnitCost() != null
                    ? batch.getUnitCost().multiply(BigDecimal.valueOf(deduct)) : BigDecimal.ZERO;
            mv.setTotalCost(lineCost);
            totalCost = totalCost.add(lineCost);
            mv.setReferenceId(referenceId);
            if (referenceType != null) {
                mv.setReferenceType(referenceType);
            }
            mv.setCreatedBy(user);
            movements.add(mv);
        }

        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock for item " + item.getItemName());
        }

        double newStock = (item.getCurrentStock() != null ? item.getCurrentStock() : 0.0) - quantity;
        item.setCurrentStock(newStock);
        itemRepository.save(item);
        movementRepository.saveAll(movements);

        StockBatch first = movements.isEmpty() ? null : movements.get(0).getBatch();

        // Commercial SALE COGS is posted by PharmacyService against the sale id (avoids double-count).
        // Non-pharmacy SALE callers may still pass a referenceId to post here.
        if ("SALE".equals(reason) && referenceId != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postCogs(
                    totalCost,
                    com.trustagro.finance.entity.BusinessUnit.PHARMACY,
                    com.trustagro.finance.entity.AccountCode.COGS_PHARMACY.getCode(),
                    "Cost of goods sold: " + item.getItemName() + " x" + quantity,
                    com.trustagro.finance.entity.FinanceSourceTypes.MODULE_INVENTORY,
                    com.trustagro.finance.entity.FinanceSourceTypes.REF_STOCK_OUT_COGS,
                    referenceId);
        }

        if (item.getReorderPoint() != null && newStock <= item.getReorderPoint()) {
            createAutoReorder(item, user);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deducted", quantity);
        result.put("newStock", newStock);
        result.put("totalCost", totalCost);
        if (first != null) {
            result.put("batchId", first.getId());
            result.put("batchNumber", first.getBatchNumber());
            result.put("expiryDate", first.getExpiryDate());
            result.put("unitCost", first.getUnitCost());
        }
        auditService.log("STOCK_OUT", "INVENTORY", "InventoryItem", itemId,
                null, "qty=" + quantity + ",reason=" + reason + ",newStock=" + newStock);
        return result;
    }

    private MovementType resolveMovementType(String reason, String override) {
        if (override != null && !override.isBlank()) {
            try {
                return MovementType.valueOf(override);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (reason != null && reason.startsWith("VETERINARY_PRESCRIPTION")) {
            return MovementType.INTERNAL_CONSUMPTION;
        }
        return "SALE".equals(reason) ? MovementType.SALE : MovementType.ISSUE;
    }

    @Transactional
    public Map<String, Object> transferStock(Long itemId, String fromZone, String toZone, Double quantity, User user) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));
        
        // Find batches in the fromZone
        List<StockBatch> fromBatches = batchRepository.findAvailableBatchesFEFO(itemId, LocalDate.now()).stream()
                .filter(b -> fromZone.equalsIgnoreCase(b.getStorageZone()))
                .toList();
        
        double remaining = quantity;
        for (StockBatch batch : fromBatches) {
            if (remaining <= 0) break;
            double move = Math.min(remaining, batch.getQuantityRemaining());
            
            // Log move
            StockMovement mv = new StockMovement();
            mv.setItem(item);
            mv.setBatch(batch);
            mv.setMovementType(MovementType.TRANSFER);
            mv.setQuantity(move);
            mv.setFromLocation(fromZone);
            mv.setToLocation(toZone);
            mv.setCreatedBy(user);
            movementRepository.save(mv);
            
            // Update batch location (simplified: assuming entire batch moves or split)
            // In a complex system we'd split the batch. For here, we'll just update the zone if it's the full remaining.
            if (move == batch.getQuantityRemaining()) {
                batch.setStorageZone(toZone);
            } else {
                // Split logic (omitted for brevity, just update zone for now)
                batch.setStorageZone(toZone);
            }
            batchRepository.save(batch);
            remaining -= move;
        }
        
        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock in zone " + fromZone);
        }
        
        return Map.of("status", "SUCCESS", "moved", quantity);
    }

    @Transactional
    public Map<String, Object> stockOut(StockOutRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);
        return stockOut(req.getItemId(), req.getQuantity(), req.getReason(), req.getReferenceId(), currentUser,
                req.getBatchId(), req.getReferenceType(), req.getMovementTypeOverride());
    }

    // ============================================================
    // AUTO REORDER
    // ============================================================

    @Transactional
    public void createAutoReorder(InventoryItem item, User user) {
        try {
            Supplier supplier = item.getPreferredSupplier() != null ? item.getPreferredSupplier() : item.getSupplier();
            if (supplier == null) {
                log.warn("Cannot create auto-reorder for {} — no supplier linked", item.getItemName());
                return;
            }

            PurchaseOrder po = new PurchaseOrder();
            po.setPoNumber("AUTO-PO-" + System.currentTimeMillis());
            po.setSupplier(supplier);
            po.setStatus(PurchaseOrderStatus.DRAFT);
            po.setOrderDate(LocalDate.now());
            po.setCreatedBy(user);
            po = poRepository.save(po);

            PurchaseOrderItem poItem = new PurchaseOrderItem();
            poItem.setPurchaseOrder(po);
            poItem.setItem(item);
            poItem.setQuantityOrdered(item.getReorderQty() != null ? item.getReorderQty() : 100.0);
            poItem.setUnitPrice(item.getLastPurchasePrice());
            if (poItem.getUnitPrice() != null && poItem.getQuantityOrdered() != null) {
                poItem.setLineTotal(poItem.getUnitPrice().multiply(BigDecimal.valueOf(poItem.getQuantityOrdered())));
            }
            poItemRepository.save(poItem);

            // Send notification to store keeper
            Notification notif = new Notification();
            notif.setTitle("Auto Reorder Generated");
            notif.setMessage(item.getItemName() + " hit reorder point. PO " + po.getPoNumber() + " created.");
            notif.setType(NotificationType.SYSTEM);
            notif.setTargetRole(Role.INVENTORY_MANAGER);
            notificationRepository.save(notif);

            log.info("Auto-reorder PO {} created for item {}", po.getPoNumber(), item.getItemName());
        } catch (Exception e) {
            log.error("Failed to create auto-reorder: {}", e.getMessage());
        }
    }

    // ============================================================
    // CYCLE COUNT
    // ============================================================

    @Transactional
    public Map<String, Object> recordCount(Long itemId, Double countedQty, User user) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));

        InventoryCount count = new InventoryCount();
        count.setItem(item);
        count.setExpectedQty(item.getCurrentStock() != null ? item.getCurrentStock() : 0.0);
        count.setCountedQty(countedQty);
        count.setCountDate(LocalDate.now());
        count.setCountedBy(user);
        count.setStatus("Pending");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("variance", countedQty - count.getExpectedQty());
        result.put("expectedQty", count.getExpectedQty());
        result.put("countedQty", countedQty);
        return result;
    }

    // ============================================================
    // DEMAND FORECASTING (Moving Average)
    // ============================================================

    @Transactional
    public Map<String, Object> generateForecast(Long itemId) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + itemId));

        // Get 90 days of SALE movements as a proxy for demand
        List<StockMovement> sales = movementRepository.findRecentSalesByItemId(itemId, LocalDate.now().minusDays(90).atStartOfDay());

        double totalSold = sales.stream().mapToDouble(StockMovement::getQuantity).sum();
        double avgMonthly = totalSold / 3.0; // 3 months
        double trend = 0;

        if (sales.size() > 1) {
            double first = sales.get(0).getQuantity();
            double last = sales.get(sales.size() - 1).getQuantity();
            trend = (last - first) / sales.size();
        }

        double predicted = Math.max(0, avgMonthly + trend);
        double confidence = Math.min(0.95, sales.size() / 30.0);

        DemandForecast forecast = new DemandForecast();
        forecast.setItem(item);
        forecast.setForecastPeriod("monthly");
        forecast.setForecastDate(LocalDate.now());
        forecast.setPredictedDemand(predicted);
        forecast.setConfidenceScore(confidence);
        forecast.setAlgorithmUsed("moving_average");
        forecastRepository.save(forecast);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", item.getItemName());
        result.put("predictedDemand", predicted);
        result.put("trend", trend);
        result.put("confidenceScore", confidence);
        result.put("recommendedOrder", Math.max(0, predicted - (item.getCurrentStock() != null ? item.getCurrentStock() : 0.0)));
        return result;
    }

    // ============================================================
    // STATS FOR DASHBOARD
    // ============================================================

    private List<Object[]> getDailyMovements(MovementType type, LocalDateTime since) {
        Map<LocalDate, Double> totals = new TreeMap<>();
        movementRepository.findAll().forEach(m -> {
            if (m.getMovementType() == type && m.getCreatedAt() != null && !m.getCreatedAt().isBefore(since)) {
                LocalDate day = m.getCreatedAt().toLocalDate();
                double qty = m.getQuantity() != null ? m.getQuantity() : 0.0;
                totals.merge(day, qty, Double::sum);
            }
        });
        List<Object[]> result = new ArrayList<>();
        totals.forEach((day, qty) -> result.add(new Object[]{day, qty}));
        return result;
    }

    public Map<String, Object> getDashboardStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        long totalSkus = itemRepository.count();
        double totalValue = itemRepository.findAll().stream()
                .mapToDouble(i -> (i.getCurrentStock() != null ? i.getCurrentStock() : 0.0)
                        * (i.getAvgUnitCost() != null ? i.getAvgUnitCost().doubleValue() : 0.0))
                .sum();
        
        long todayStockInCount = movementRepository.countByMovementTypeAndCreatedAtAfter(MovementType.RECEIPT, todayStart);
        long todayStockOutCount = movementRepository.countByMovementTypeAndCreatedAtAfter(MovementType.ISSUE, todayStart) 
                                + movementRepository.countByMovementTypeAndCreatedAtAfter(MovementType.SALE, todayStart);

        List<InventoryItemResponse> lowStock = itemRepository.findAll().stream()
                .filter(i -> i.getReorderPoint() != null && i.getCurrentStock() != null && i.getCurrentStock() <= i.getReorderPoint())
                .map(this::mapToResponse)
                .toList();

        long pendingRequisitions = requisitionRepository.countByStatus(RequisitionStatus.REQUESTED);
        
        // Turnover Calculation: (Total Out last 30 days) / Avg Stock level
        LocalDateTime monthAgo = LocalDate.now().minusDays(30).atStartOfDay();
        double totalOutValue = movementRepository.findAll().stream()
                .filter(m -> (m.getMovementType() == MovementType.SALE || m.getMovementType() == MovementType.ISSUE || m.getMovementType() == MovementType.INTERNAL_CONSUMPTION)
                        && m.getCreatedAt().isAfter(monthAgo))
                .mapToDouble(m -> m.getTotalCost() != null ? m.getTotalCost().doubleValue() : 0.0)
                .sum();
        
        double turnoverRate = totalValue > 0 ? (totalOutValue / totalValue) * 100 : 0.0;

        List<StockBatch> expiring = batchRepository.findExpiringBatches(LocalDate.now(), LocalDate.now().plusDays(30));
        
        List<Map<String, Object>> expiringMap = expiring.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("item_name", b.getItem().getItemName());
            m.put("batch_number", b.getBatchNumber());
            m.put("quantity_remaining", b.getQuantityRemaining());
            m.put("expiry_date", b.getExpiryDate());
            return m;
        }).toList();

        // Recent Stock In (Detailed for the new table)
        List<Map<String, Object>> recentStockIn = movementRepository.findLatestStockIn(org.springframework.data.domain.PageRequest.of(0, 10))
                .stream().map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", m.getCreatedAt());
                    map.put("item", m.getItem().getItemName());
                    map.put("batch", m.getBatch() != null ? m.getBatch().getBatchNumber() : "N/A");
                    map.put("quantity", m.getQuantity());
                    map.put("warehouse", m.getBatch() != null ? m.getBatch().getStorageZone() : "N/A");
                    map.put("supplier", m.getItem().getSupplier() != null ? m.getItem().getSupplier().getName() : "N/A");
                    return map;
                }).toList();

        // Movement Trend Data (Last 7 days) — computed in Java for H2/PostgreSQL compatibility
        List<Object[]> inTrend = getDailyMovements(MovementType.RECEIPT, weekAgo);
        List<Object[]> outTrend = getDailyMovements(MovementType.SALE, weekAgo);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_skus", totalSkus);
        stats.put("total_value", totalValue);
        stats.put("today_stock_in", todayStockInCount);
        stats.put("today_stock_out", todayStockOutCount);
        stats.put("low_stock_count", lowStock.size());
        stats.put("expiring_count", expiringMap.size());
        stats.put("pending_requisitions", pendingRequisitions);
        stats.put("inventory_turnover", turnoverRate);
        
        stats.put("low_stock", lowStock);
        stats.put("expiring", expiringMap);
        stats.put("recent_stock_in", recentStockIn);
        stats.put("movement_trend", Map.of("in", inTrend, "out", outTrend));

        // Category Cards (Requirement 1.4)
        Map<ItemCategory, Double> categoryValue = new HashMap<>();
        Map<ItemCategory, Long> categoryCount = new HashMap<>();
        itemRepository.findAll().forEach(i -> {
            if (i.getCategory() != null) {
                categoryCount.put(i.getCategory(), categoryCount.getOrDefault(i.getCategory(), 0L) + 1);
                double val = (i.getCurrentStock() != null ? i.getCurrentStock() : 0.0) 
                           * (i.getAvgUnitCost() != null ? i.getAvgUnitCost().doubleValue() : 0.0);
                categoryValue.put(i.getCategory(), categoryValue.getOrDefault(i.getCategory(), 0.0) + val);
            }
        });
        
        List<Map<String, Object>> categoryDistribution = new ArrayList<>();
        for (ItemCategory cat : ItemCategory.values()) {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("category", cat.name());
            cMap.put("count", categoryCount.getOrDefault(cat, 0L));
            cMap.put("value", categoryValue.getOrDefault(cat, 0.0));
            categoryDistribution.add(cMap);
        }
        stats.put("category_stats", categoryDistribution);

        // Activity Timeline (Vertical feed)
        List<Map<String, Object>> timeline = movementRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(15)
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("time", m.getCreatedAt());
                    map.put("action", m.getMovementType());
                    map.put("item", m.getItem().getItemName());
                    map.put("qty", m.getQuantity());
                    map.put("user", m.getCreatedBy() != null ? m.getCreatedBy().getFullName() : "System");
                    return map;
                })
                .toList();
        stats.put("activity_timeline", timeline);

        return stats;
    }

    public List<InventoryItemResponse> getAllItems() {
        return itemRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<InventoryItemResponse> getLowStockItems() {
        return itemRepository.findAll().stream()
                .filter(i -> i.getReorderPoint() != null && i.getCurrentStock() != null && i.getCurrentStock() <= i.getReorderPoint())
                .map(this::mapToResponse).toList();
    }

    public List<StockBatch> getExpiringItems() {
        return batchRepository.findExpiringBatches(LocalDate.now(), LocalDate.now().plusDays(30));
    }

    private InventoryItemResponse mapToResponse(InventoryItem item) {
        InventoryItemResponse r = new InventoryItemResponse();
        r.setId(item.getId());
        r.setItemName(item.getItemName());
        r.setName(item.getItemName());
        r.setSku(item.getSku());
        r.setCategory(item.getCategory());
        r.setCurrentStock(item.getCurrentStock());
        r.setReorderPoint(item.getReorderPoint());
        r.setMinimumStockLevel(item.getMinimumStockLevel());
        r.setReorderQty(item.getReorderQty());
        r.setStatus(item.getStatus());
        r.setUnit(item.getUnit() != null ? item.getUnit().name() : "");
        r.setRetailPrice(item.getRetailPrice());
        r.setWholesalePrice(item.getWholesalePrice());
        r.setPartnerPrice(item.getPartnerPrice());
        r.setSellingPrice(item.getSellingPrice());
        r.setAvgUnitCost(item.getAvgUnitCost());
        r.setIsControlled(item.getIsControlled());
        BigDecimal unitPrice = item.getRetailPrice() != null ? item.getRetailPrice() : BigDecimal.ZERO;
        double stock = item.getCurrentStock() != null ? item.getCurrentStock() : 0.0;
        r.setInventoryValue(unitPrice.multiply(BigDecimal.valueOf(stock)).doubleValue());
        return r;
    }

    public List<InventoryItemResponse> getPharmacyCatalog() {
        return itemRepository.findAll().stream()
                .filter(i -> i.getStatus() == ItemStatus.ACTIVE)
                .filter(i -> PHARMACY_POS_CATEGORIES.contains(i.getCategory()))
                .map(item -> {
                    InventoryItemResponse r = mapToResponse(item);
                    List<StockBatch> batches = batchRepository.findAvailableBatchesFEFO(item.getId(), LocalDate.now());
                    if (!batches.isEmpty()) {
                        StockBatch batch = batches.get(0);
                        r.setBatchNumber(batch.getBatchNumber());
                        r.setExpiryDate(batch.getExpiryDate());
                    }
                    return r;
                })
                .toList();
    }
}
