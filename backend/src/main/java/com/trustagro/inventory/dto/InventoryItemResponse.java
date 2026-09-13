package com.trustagro.inventory.dto;

import com.trustagro.inventory.entity.ItemCategory;
import com.trustagro.inventory.entity.ItemStatus;
import com.trustagro.inventory.entity.ItemUnit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryItemResponse {
    private Long id;
    private String itemName;
    private ItemCategory category;
    private ItemUnit unit;
    private String sku;
    private Double minimumStockLevel;
    private Double reorderPoint;
    private Double reorderQty;
    private String storageLocation;
    private boolean expiryRequired;
    private ItemStatus status;
    private Double currentStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Pricing (shared inventory ↔ pharmacy ↔ finance)
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal partnerPrice;
    private BigDecimal sellingPrice;
    private BigDecimal avgUnitCost;
    private Boolean isControlled;

    // Pharmacy catalog enrichment (nearest FEFO batch)
    private String batchNumber;
    private LocalDate expiryDate;
    private Double inventoryValue;

    // Added for frontend compatibility
    private String name;
    private String unitString;

    public Long getId() { return id; }
    public String getItemName() { return itemName; }
    public String getSku() { return sku; }
    public ItemCategory getCategory() { return category; }
    public Double getCurrentStock() { return currentStock; }

    public void setId(Long id) { this.id = id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setSku(String sku) { this.sku = sku; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }
    public ItemUnit getUnit() { return unit; }
    public void setUnit(ItemUnit unit) { this.unit = unit; }
    
    // Explicit for frontend compatibility if mapped from String
    public void setName(String name) { this.name = name; this.itemName = name; }
    public String getName() { return this.name != null ? this.name : this.itemName; }
    public void setUnit(String unitString) { this.unitString = unitString; }
    public String getUnitString() { return this.unitString; }
    
    public void setRetailPrice(java.math.BigDecimal retailPrice) { this.retailPrice = retailPrice; }
    public void setWholesalePrice(java.math.BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }
    public void setPartnerPrice(java.math.BigDecimal partnerPrice) { this.partnerPrice = partnerPrice; }
    public void setSellingPrice(java.math.BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public void setAvgUnitCost(java.math.BigDecimal avgUnitCost) { this.avgUnitCost = avgUnitCost; }
    public void setIsControlled(Boolean isControlled) { this.isControlled = isControlled; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public void setExpiryDate(java.time.LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public void setInventoryValue(Double inventoryValue) { this.inventoryValue = inventoryValue; }
    public void setMinimumStockLevel(Double minimumStockLevel) { this.minimumStockLevel = minimumStockLevel; }
    public void setReorderQty(Double reorderQty) { this.reorderQty = reorderQty; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public java.math.BigDecimal getRetailPrice() { return retailPrice; }
    public java.math.BigDecimal getWholesalePrice() { return wholesalePrice; }
    public java.math.BigDecimal getPartnerPrice() { return partnerPrice; }
    public java.math.BigDecimal getSellingPrice() { return sellingPrice; }
    public java.math.BigDecimal getAvgUnitCost() { return avgUnitCost; }
    public Boolean getIsControlled() { return isControlled; }
    public String getBatchNumber() { return batchNumber; }
    public java.time.LocalDate getExpiryDate() { return expiryDate; }
    public Double getInventoryValue() { return inventoryValue; }
}
