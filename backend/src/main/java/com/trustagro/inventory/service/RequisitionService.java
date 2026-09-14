package com.trustagro.inventory.service;

import com.trustagro.farm.repository.BarnRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.inventory.entity.InternalRequisition;
import com.trustagro.inventory.entity.RequisitionItem;
import com.trustagro.inventory.entity.RequisitionStatus;
import com.trustagro.inventory.repository.InternalRequisitionRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RequisitionService {

    private final InternalRequisitionRepository requisitionRepository;
    private final InventoryItemRepository itemRepository;
    private final FarmRepository farmRepository;
    private final BarnRepository barnRepository;
    private final FlockRepository flockRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;

    public RequisitionService(InternalRequisitionRepository requisitionRepository,
                              InventoryItemRepository itemRepository,
                              FarmRepository farmRepository,
                              BarnRepository barnRepository,
                              FlockRepository flockRepository,
                              InventoryService inventoryService,
                              FinanceService financeService) {
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.farmRepository = farmRepository;
        this.barnRepository = barnRepository;
        this.flockRepository = flockRepository;
        this.inventoryService = inventoryService;
        this.financeService = financeService;
    }

    public List<InternalRequisition> getAllRequisitions() {
        return requisitionRepository.findAll();
    }

    @Transactional
    public InternalRequisition createRequisition(Map<String, Object> data, User user) {
        InternalRequisition req = new InternalRequisition();
        req.setRequisitionNumber("REQ-" + System.currentTimeMillis());
        
        if (data.containsKey("farmId")) req.setFarm(farmRepository.findById(Long.valueOf(data.get("farmId").toString())).orElse(null));
        if (data.containsKey("barnId")) req.setBarn(barnRepository.findById(Long.valueOf(data.get("barnId").toString())).orElse(null));
        if (data.containsKey("flockId")) req.setFlock(flockRepository.findById(Long.valueOf(data.get("flockId").toString())).orElse(null));
        
        req.setNotes(data.getOrDefault("notes", "").toString());
        req.setRequestedBy(user);
        req.setStatus(RequisitionStatus.REQUESTED);

        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Map<String, Object> itemData : items) {
            RequisitionItem ri = new RequisitionItem();
            ri.setRequisition(req);
            ri.setItem(itemRepository.findById(Long.valueOf(itemData.get("itemId").toString())).orElseThrow());
            ri.setQuantityRequested(Double.valueOf(itemData.get("quantity").toString()));
            
            // Expected current cost
            ri.setUnitCost(ri.getItem().getAvgUnitCost());
            if (ri.getUnitCost() != null) {
                ri.setTotalCost(ri.getUnitCost().multiply(BigDecimal.valueOf(ri.getQuantityRequested())));
                totalValue = totalValue.add(ri.getTotalCost());
            }
            req.getItems().add(ri);
        }
        
        req.setTotalValue(totalValue);
        
        return requisitionRepository.save(req);
    }

    @Transactional
    public InternalRequisition approveRequisition(Long reqId, User user) {
        InternalRequisition req = requisitionRepository.findById(reqId).orElseThrow();
        req.setStatus(RequisitionStatus.APPROVED);
        req.setApprovedBy(user);
        req.setApprovedAt(LocalDateTime.now());
        
        // For simplicity, approve requested quantity fully. Can be expanded for partial.
        req.getItems().forEach(i -> i.setQuantityApproved(i.getQuantityRequested()));
        
        return requisitionRepository.save(req);
    }

    @Transactional
    public InternalRequisition issueRequisition(Long reqId, User user) {
        InternalRequisition req = requisitionRepository.findById(reqId).orElseThrow();
        if (req.getStatus() != RequisitionStatus.APPROVED) {
            throw new IllegalStateException("Requisition must be APPROVED before issuing");
        }
        
        req.setStatus(RequisitionStatus.ISSUED);
        req.setIssuedBy(user);
        req.setIssuedAt(LocalDateTime.now());

        for (RequisitionItem ri : req.getItems()) {
            ri.setQuantityIssued(ri.getQuantityApproved());
            // Call InventoryService to physically deduct stock
            // This already creates a movement record
            inventoryService.stockOut(ri.getItem().getId(), ri.getQuantityIssued(), "REQUISITION", req.getId(), user);
            
            // Automated Finance Logging for internal consumption — credit inventory, not cash
            if (ri.getUnitCost() != null && ri.getUnitCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
                String desc = "Requisition fulfillment: " + ri.getItem().getItemName() + " x" + ri.getQuantityIssued();
                if (req.getFlock() != null) desc += " for Flock " + req.getFlock().getBatchCode();
                else if (req.getFarm() != null) desc += " for Farm " + req.getFarm().getFarmName();

                // One posting per requisition item line via composite reference: requisitionId * 100000 + itemId
                Long lineRef = req.getId() * 100_000L + ri.getItem().getId();
                financeService.postInventoryConsumption(
                        ri.getUnitCost().multiply(java.math.BigDecimal.valueOf(ri.getQuantityIssued())),
                        com.trustagro.finance.entity.BusinessUnit.GENERAL,
                        "5120",
                        desc,
                        com.trustagro.finance.entity.FinanceSourceTypes.MODULE_INVENTORY,
                        com.trustagro.finance.entity.FinanceSourceTypes.REF_REQUISITION,
                        lineRef
                );
            }
        }

        return requisitionRepository.save(req);
    }
}
