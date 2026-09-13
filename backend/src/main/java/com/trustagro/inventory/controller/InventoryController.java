package com.trustagro.inventory.controller;

import com.trustagro.inventory.dto.InventoryItemResponse;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.ItemCategory;
import com.trustagro.inventory.entity.ItemStatus;
import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.entity.StockMovement;
import com.trustagro.inventory.repository.*;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;
    private final InventoryItemRepository itemRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;

    // Categories that pharmacy can dispense
    private static final Set<ItemCategory> PHARMACY_CATEGORIES =
            Set.of(ItemCategory.DRUG, ItemCategory.VACCINE);

    public InventoryController(InventoryService inventoryService,
                               InventoryItemRepository itemRepository,
                               StockBatchRepository batchRepository,
                               StockMovementRepository movementRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // ─── Dashboard Stats ──────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(inventoryService.getDashboardStats());
    }

    // ─── Items ────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<?> getAllItems() {
        try {
            List<InventoryItemResponse> result = inventoryService.getAllItems();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("getAllItems failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<InventoryItem> getItem(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(itemRepository.save(item));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable Long id, @RequestBody InventoryItem item) {
        item.setId(id);
        return ResponseEntity.ok(itemRepository.save(item));
    }

    // ─── Low Stock / Expiry ───────────────────────────────────────────────────

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItem>> getLowStock() {
        List<InventoryItem> lowStock = itemRepository.findAll().stream()
                .filter(i -> i.getReorderPoint() != null && i.getCurrentStock() != null
                        && i.getCurrentStock() <= i.getReorderPoint())
                .toList();
        return ResponseEntity.ok(lowStock);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<StockBatch>> getExpiring() {
        return ResponseEntity.ok(batchRepository.findExpiringBatches(LocalDate.now(), LocalDate.now().plusDays(30)));
    }

    // ─── Barcode Scanning ─────────────────────────────────────────────────────

    @GetMapping("/scan/{barcode}")
    public ResponseEntity<Map<String, Object>> scanItem(@PathVariable String barcode, Authentication auth) {
        return ResponseEntity.ok(inventoryService.scanItem(barcode, getUser(auth)));
    }

    @PostMapping("/scan-action")
    public ResponseEntity<Map<String, Object>> processScanAction(@RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(inventoryService.processScanAction(data, getUser(auth)));
    }

    // ─── Stock In (FIXED) ─────────────────────────────────────────────────────

    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<Map<String, Object>> stockIn(@RequestBody Map<String, Object> data, Authentication auth) {
        try {
            // Null-safe field extraction
            if (data.get("itemId") == null || data.get("quantity") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "itemId and quantity are required"));
            }

            Long itemId;
            Double qty;
            try {
                itemId = Long.valueOf(data.get("itemId").toString());
                qty = Double.valueOf(data.get("quantity").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "itemId must be a number and quantity must be a decimal"));
            }

            if (qty <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "quantity must be greater than zero"));
            }

            Map<String, Object> result = inventoryService.stockIn(itemId, qty, data, getUser(auth));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("STOCK_IN validation error: {} | body={}", e.getMessage(), data);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("STOCK_IN_ERROR: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Stock-in failed: " + e.getMessage()));
        }
    }

    // ─── Stock Out (FIXED) ────────────────────────────────────────────────────

    @PostMapping("/stock-out")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<Map<String, Object>> stockOut(@RequestBody Map<String, Object> data, Authentication auth) {
        try {
            // Null-safe field extraction
            if (data.get("itemId") == null || data.get("quantity") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "itemId and quantity are required"));
            }

            Long itemId;
            Double qty;
            try {
                itemId = Long.valueOf(data.get("itemId").toString());
                qty = Double.valueOf(data.get("quantity").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "itemId must be a number and quantity must be a decimal"));
            }

            if (qty <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "quantity must be greater than zero"));
            }

            String reason = data.getOrDefault("reason", "ISSUE").toString();
            Map<String, Object> result = inventoryService.stockOut(itemId, qty, reason, null, getUser(auth));
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("STOCK_OUT validation error: {} | body={}", e.getMessage(), data);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (com.trustagro.common.exception.ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("STOCK_OUT_ERROR: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Stock-out failed: " + e.getMessage()));
        }
    }

    // ─── Stock Movements ──────────────────────────────────────────────────────

    @GetMapping("/movements/recent")
    public ResponseEntity<List<StockMovement>> getRecentMovements() {
        return ResponseEntity.ok(movementRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(50).toList());
    }

    @GetMapping("/movements/item/{itemId}")
    public ResponseEntity<List<StockMovement>> getItemMovements(@PathVariable Long itemId) {
        return ResponseEntity.ok(movementRepository.findByItemId(itemId));
    }

    // ─── Purchase Orders ──────────────────────────────────────────────────────

    @GetMapping("/purchase-orders")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrders() {
        return ResponseEntity.ok(purchaseOrderRepository.findAll());
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@RequestBody PurchaseOrder po, Authentication auth) {
        po.setCreatedBy(getUser(auth));
        return ResponseEntity.ok(purchaseOrderRepository.save(po));
    }

    @PutMapping("/purchase-orders/{id}/send")
    public ResponseEntity<PurchaseOrder> sendPO(@PathVariable Long id) {
        return purchaseOrderRepository.findById(id).map(po -> {
            po.setStatus("Sent");
            return ResponseEntity.ok(purchaseOrderRepository.save(po));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/purchase-orders/{id}/receive")
    public ResponseEntity<PurchaseOrder> receivePO(@PathVariable Long id) {
        return purchaseOrderRepository.findById(id).map(po -> {
            po.setStatus("Received");
            po.setActualDelivery(LocalDate.now());
            return ResponseEntity.ok(purchaseOrderRepository.save(po));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── Cycle Count ──────────────────────────────────────────────────────────

    @PostMapping("/count")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<Map<String, Object>> recordCount(@RequestBody Map<String, Object> data, Authentication auth) {
        try {
            if (data.get("itemId") == null || data.get("countedQty") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "itemId and countedQty are required"));
            }
            Long itemId = Long.valueOf(data.get("itemId").toString());
            Double counted = Double.valueOf(data.get("countedQty").toString());
            return ResponseEntity.ok(inventoryService.recordCount(itemId, counted, getUser(auth)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Forecasting ──────────────────────────────────────────────────────────

    @GetMapping("/forecast/{itemId}")
    public ResponseEntity<Map<String, Object>> getForecast(@PathVariable Long itemId) {
        return ResponseEntity.ok(inventoryService.generateForecast(itemId));
    }

    // ─── Batches ──────────────────────────────────────────────────────────────

    @GetMapping("/batches/{itemId}")
    public ResponseEntity<List<StockBatch>> getBatches(@PathVariable Long itemId) {
        return ResponseEntity.ok(batchRepository.findAvailableBatchesFEFO(itemId, LocalDate.now()));
    }

    // ─── Sellable Items for Pharmacy ──────────────────────────────────────────

    @GetMapping("/sellable-items")
    public ResponseEntity<List<InventoryItem>> getSellableItems() {
        return ResponseEntity.ok(itemRepository.findAll().stream()
                .filter(i -> i.getStatus() == ItemStatus.ACTIVE && i.getCurrentStock() != null && i.getCurrentStock() > 0)
                .toList());
    }

    // ─── Pharmacy Integration ─────────────────────────────────────────────────

    /**
     * Returns only drug/vaccine items with stock > 0 for the pharmacy POS.
     */
    @GetMapping("/pharmacy-items")
    public ResponseEntity<List<InventoryItemResponse>> getPharmacyItems() {
        return ResponseEntity.ok(inventoryService.getPharmacyCatalog().stream()
                .filter(i -> i.getCurrentStock() != null && i.getCurrentStock() > 0)
                .toList());
    }

    /**
     * Called by PharmacyService on each sale to deduct from inventory stock.
     */
    @PostMapping("/pharmacy-deduct")
    public ResponseEntity<Map<String, Object>> pharmacyDeduct(@RequestBody Map<String, Object> data, Authentication auth) {
        try {
            if (data.get("itemId") == null || data.get("quantity") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "itemId and quantity are required"));
            }
            Long itemId = Long.valueOf(data.get("itemId").toString());
            Double qty = Double.valueOf(data.get("quantity").toString());
            if (qty <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "quantity must be greater than zero"));
            }
            Map<String, Object> result = inventoryService.stockOut(itemId, qty, "SALE", null, getUser(auth));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("PHARMACY_DEDUCT validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("PHARMACY_DEDUCT_ERROR: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Deduction failed: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('INVENTORY_MANAGER')")
    public ResponseEntity<Map<String, Object>> transferStock(@RequestBody Map<String, Object> data, Authentication auth) {
        Long itemId = Long.valueOf(data.get("itemId").toString());
        String fromZone = data.get("fromZone").toString();
        String toZone = data.get("toZone").toString();
        Double quantity = Double.valueOf(data.get("quantity").toString());
        return ResponseEntity.ok(inventoryService.transferStock(itemId, fromZone, toZone, quantity, getUser(auth)));
    }
}
