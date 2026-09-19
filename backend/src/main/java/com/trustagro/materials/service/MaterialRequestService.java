package com.trustagro.materials.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.materials.dto.*;
import com.trustagro.materials.entity.*;
import com.trustagro.materials.repository.MaterialGoodsReceiptRepository;
import com.trustagro.materials.repository.MaterialIssueEventRepository;
import com.trustagro.materials.repository.MaterialRequestDisbursementRepository;
import com.trustagro.materials.repository.MaterialRequestRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.dto.PurchaseOrderResponse;
import com.trustagro.procurement.dto.RequisitionRequest;
import com.trustagro.procurement.dto.RequisitionResponse;
import com.trustagro.procurement.service.ProcNumberService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialRequestService {

    public static final String AUDIT_MODULE = "MATERIALS";

    private static final Set<MaterialRequestStatus> OPEN_DUPLICATE_STATUSES = EnumSet.of(
            MaterialRequestStatus.SUBMITTED,
            MaterialRequestStatus.FINANCE_REVIEW,
            MaterialRequestStatus.APPROVED,
            MaterialRequestStatus.READY_FOR_STORE,
            MaterialRequestStatus.PURCHASE_AUTHORIZED
    );

    private static final Set<MaterialRequestStatus> STOCK_POSTED_STATUSES = EnumSet.of(
            MaterialRequestStatus.GOODS_RECEIVED,
            MaterialRequestStatus.STOCK_REGISTERED,
            MaterialRequestStatus.PARTIALLY_FULFILLED,
            MaterialRequestStatus.ISSUED,
            MaterialRequestStatus.ACKNOWLEDGED,
            MaterialRequestStatus.FULFILLED
    );

    private final MaterialRequestRepository requestRepository;
    private final MaterialRequestDisbursementRepository disbursementRepository;
    private final MaterialGoodsReceiptRepository goodsReceiptRepository;
    private final MaterialIssueEventRepository issueEventRepository;
    private final InventoryItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final FarmRepository farmRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final ProcurementService procurementService;
    private final ProcNumberService numberService;
    private final FarmAccessService farmAccessService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<MaterialRequestResponse> list(MaterialRequestStatus status, MaterialRequestType type,
                                              Long farmId, Long requesterId, String queue) {
        User me = farmAccessService.currentUser();
        if ("MY".equalsIgnoreCase(queue)) {
            return requestRepository.findByRequesterIdOrderByCreatedAtDesc(me.getId()).stream()
                    .map(this::toResponse).collect(Collectors.toList());
        }
        if ("FINANCE".equalsIgnoreCase(queue)) {
            return requestRepository.findByStatusInOrderByCreatedAtDesc(List.of(
                    MaterialRequestStatus.SUBMITTED, MaterialRequestStatus.FINANCE_REVIEW
            )).stream().map(this::toResponse).collect(Collectors.toList());
        }
        if ("STORE".equalsIgnoreCase(queue)) {
            return requestRepository.findByStatusInOrderByCreatedAtDesc(List.of(
                    MaterialRequestStatus.READY_FOR_STORE,
                    MaterialRequestStatus.APPROVED,
                    MaterialRequestStatus.PARTIALLY_FULFILLED
            )).stream()
                    .filter(r -> r.getType() == MaterialRequestType.INTERNAL_ISSUE_REQUEST)
                    .map(this::toResponse).collect(Collectors.toList());
        }
        if ("PURCHASE".equalsIgnoreCase(queue)) {
            return requestRepository.findByStatusInOrderByCreatedAtDesc(List.of(
                    MaterialRequestStatus.APPROVED,
                    MaterialRequestStatus.PURCHASE_AUTHORIZED,
                    MaterialRequestStatus.PAYMENT_SENT
            )).stream()
                    .filter(r -> r.getType() == MaterialRequestType.PURCHASE_REQUEST)
                    .map(this::toResponse).collect(Collectors.toList());
        }
        return requestRepository.filter(status, type, farmId, requesterId, null, null, null).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialRequestResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public MaterialRequestResponse create(MaterialRequestCreateRequest req) {
        User me = farmAccessService.currentUser();
        MaterialRequestType type = parseType(req.getType());
        MaterialRequest mr = new MaterialRequest();
        mr.setRequestNumber(numberService.nextMaterialRequest());
        mr.setType(type);
        mr.setStatus(MaterialRequestStatus.DRAFT);
        mr.setRequester(me);
        mr.setRequestDate(req.getRequestDate() != null ? req.getRequestDate() : LocalDate.now());
        applyFields(mr, req);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_CREATED", AUDIT_MODULE, "MaterialRequest", saved.getId(), null, saved.getRequestNumber());
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse update(Long id, MaterialRequestCreateRequest req) {
        MaterialRequest mr = find(id);
        if (mr.getStatus() != MaterialRequestStatus.DRAFT && mr.getStatus() != MaterialRequestStatus.RETURNED) {
            throw new BusinessException("Only DRAFT or RETURNED requests can be updated");
        }
        if (mr.getStatus() == MaterialRequestStatus.RETURNED) {
            mr.setStatus(MaterialRequestStatus.DRAFT);
            mr.setRejectionReason(null);
        }
        applyFields(mr, req);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_UPDATED", AUDIT_MODULE, "MaterialRequest", saved.getId(), null, saved.getRequestNumber());
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse submit(Long id) {
        MaterialRequest mr = find(id);
        if (mr.getStatus() != MaterialRequestStatus.DRAFT && mr.getStatus() != MaterialRequestStatus.RETURNED) {
            throw new BusinessException("Only DRAFT or RETURNED requests can be submitted");
        }
        if (mr.getLines().isEmpty()) {
            throw new BusinessException("Request has no line items");
        }
        assertNoDuplicate(mr);
        String old = mr.getStatus().name();
        mr.setStatus(MaterialRequestStatus.FINANCE_REVIEW);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_SUBMITTED", AUDIT_MODULE, "MaterialRequest", saved.getId(), old, saved.getStatus().name());
        notificationService.createRoleNotification(
                "Material request submitted",
                saved.getRequestNumber() + " awaiting finance review",
                Role.FINANCE);
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse financeApprove(Long id, FinanceDecisionRequest body) {
        MaterialRequest mr = find(id);
        User me = farmAccessService.currentUser();
        if (mr.getStatus() != MaterialRequestStatus.FINANCE_REVIEW
                && mr.getStatus() != MaterialRequestStatus.SUBMITTED) {
            throw new BusinessException("Request is not awaiting finance approval");
        }
        if (mr.getRequester() != null && mr.getRequester().getId().equals(me.getId())) {
            throw new BusinessException("Cannot approve your own material request (SoD)");
        }
        String old = mr.getStatus().name();
        BigDecimal approved = body != null && body.getApprovedAmount() != null
                ? body.getApprovedAmount()
                : (mr.getEstimatedTotal() != null ? mr.getEstimatedTotal() : BigDecimal.ZERO);
        mr.setApprovedAmount(approved.setScale(2, RoundingMode.HALF_UP));
        mr.setFinanceApprovedBy(me);
        mr.setFinanceApprovedAt(LocalDateTime.now());
        for (MaterialRequestLine line : mr.getLines()) {
            if (line.getQuantityApproved() == null || line.getQuantityApproved().compareTo(BigDecimal.ZERO) <= 0) {
                line.setQuantityApproved(line.getQuantityRequested());
            }
        }
        if (mr.getType() == MaterialRequestType.INTERNAL_ISSUE_REQUEST) {
            mr.setStatus(MaterialRequestStatus.READY_FOR_STORE);
        } else {
            mr.setStatus(MaterialRequestStatus.APPROVED);
        }
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_FINANCE_APPROVED", AUDIT_MODULE, "MaterialRequest", saved.getId(), old, saved.getStatus().name());
        if (saved.getRequester() != null) {
            notificationService.createUserNotification(
                    "Material request approved",
                    saved.getRequestNumber() + " approved by finance",
                    saved.getRequester());
        }
        notificationService.createRoleNotification(
                "Material request approved",
                saved.getRequestNumber() + " ready for next step",
                Role.INVENTORY_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse financeReject(Long id, FinanceDecisionRequest body) {
        return financeDecision(id, MaterialRequestStatus.REJECTED, body, "MR_FINANCE_REJECTED");
    }

    @Transactional
    public MaterialRequestResponse returnForCorrection(Long id, FinanceDecisionRequest body) {
        return financeDecision(id, MaterialRequestStatus.RETURNED, body, "MR_RETURNED");
    }

    @Transactional
    public MaterialRequestResponse authorizePurchase(Long id, AuthorizePurchaseRequest body) {
        MaterialRequest mr = find(id);
        if (mr.getType() != MaterialRequestType.PURCHASE_REQUEST) {
            throw new BusinessException("Only PURCHASE_REQUEST can be authorized for purchase");
        }
        if (mr.getStatus() != MaterialRequestStatus.APPROVED
                && mr.getStatus() != MaterialRequestStatus.PAYMENT_SENT) {
            throw new BusinessException("Purchase authorization requires APPROVED status");
        }
        Long supplierId = body != null && body.getSupplierId() != null
                ? body.getSupplierId()
                : (mr.getPreferredSupplier() != null ? mr.getPreferredSupplier().getId() : null);
        if (supplierId == null) {
            throw new BusinessException("supplierId is required to authorize purchase");
        }

        RequisitionRequest req = new RequisitionRequest();
        req.setTitle("Material request " + mr.getRequestNumber());
        req.setJustification(mr.getReason());
        req.setNeededBy(mr.getNeededBy());
        req.setPriority(mr.getPriority());
        List<RequisitionRequest.RequisitionItemRequest> items = new ArrayList<>();
        for (MaterialRequestLine line : mr.getLines()) {
            RequisitionRequest.RequisitionItemRequest ri = new RequisitionRequest.RequisitionItemRequest();
            ri.setInventoryItemId(line.getInventoryItem().getId());
            ri.setQuantityRequested(line.getQuantityApproved() != null && line.getQuantityApproved().compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityApproved().doubleValue()
                    : line.getQuantityRequested().doubleValue());
            ri.setEstimatedUnitPrice(line.getEstimatedUnitPrice());
            ri.setNotes(line.getNotes());
            items.add(ri);
        }
        req.setItems(items);

        RequisitionResponse created = procurementService.createRequisition(req);
        Long reqId = created.getId();
        procurementService.submitRequisition(reqId);
        try {
            procurementService.reviewRequisition(reqId);
        } catch (BusinessException ignored) {
            // SUBMITTED → APPROVED is allowed directly
        }

        PurchaseOrderResponse po = null;
        try {
            procurementService.approveRequisition(reqId);
            po = procurementService.convertRequisitionToPo(reqId, supplierId);
        } catch (BusinessException approveOrConvertEx) {
            // Self-approve SoD or lifecycle — fall back to draft PO from material lines
            com.trustagro.procurement.dto.PurchaseOrderRequest poReq =
                    new com.trustagro.procurement.dto.PurchaseOrderRequest();
            poReq.setSupplierId(supplierId);
            poReq.setNotes("From material request " + mr.getRequestNumber()
                    + " (proc requisition " + created.getRequisitionNumber() + ")");
            List<com.trustagro.procurement.dto.PurchaseOrderRequest.Line> poItems = new ArrayList<>();
            for (MaterialRequestLine line : mr.getLines()) {
                com.trustagro.procurement.dto.PurchaseOrderRequest.Line pi =
                        new com.trustagro.procurement.dto.PurchaseOrderRequest.Line();
                pi.setInventoryItemId(line.getInventoryItem().getId());
                BigDecimal qty = nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                        ? line.getQuantityApproved() : line.getQuantityRequested();
                pi.setQuantityOrdered(qty.doubleValue());
                pi.setUnitPrice(line.getEstimatedUnitPrice() != null
                        ? line.getEstimatedUnitPrice() : BigDecimal.ZERO);
                pi.setNotes(line.getNotes());
                poItems.add(pi);
            }
            poReq.setItems(poItems);
            po = procurementService.createPurchaseOrder(poReq);
        }

        String old = mr.getStatus().name();
        mr.setProcRequisitionId(reqId);
        mr.setPurchaseOrderId(po.getId());
        for (MaterialRequestLine line : mr.getLines()) {
            BigDecimal approved = nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityApproved() : line.getQuantityRequested();
            line.setQuantityOrdered(approved);
        }
        mr.setStatus(MaterialRequestStatus.PURCHASE_AUTHORIZED);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_PURCHASE_AUTHORIZED", AUDIT_MODULE, "MaterialRequest", saved.getId(), old, saved.getStatus().name());
        notificationService.createRoleNotification(
                "Purchase authorized",
                saved.getRequestNumber() + " linked to PO " + po.getPoNumber(),
                Role.INVENTORY_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse recordDisbursement(Long id, DisbursementRequest body) {
        MaterialRequest mr = find(id);
        if (mr.getType() != MaterialRequestType.PURCHASE_REQUEST) {
            throw new BusinessException("Disbursement is only for PURCHASE_REQUEST");
        }
        if (mr.getStatus() != MaterialRequestStatus.APPROVED
                && mr.getStatus() != MaterialRequestStatus.PURCHASE_AUTHORIZED
                && mr.getStatus() != MaterialRequestStatus.PAYMENT_SENT) {
            throw new BusinessException("Disbursement requires APPROVED or PURCHASE_AUTHORIZED status");
        }
        if (body == null || body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Disbursement amount must be positive");
        }

        if (body.getDisbursementNumber() != null && !body.getDisbursementNumber().isBlank()) {
            Optional<MaterialRequestDisbursement> existing =
                    disbursementRepository.findByDisbursementNumber(body.getDisbursementNumber().trim());
            if (existing.isPresent()) {
                return toResponse(mr);
            }
        }

        BigDecimal amount = body.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal approved = nz(mr.getApprovedAmount());
        BigDecimal paid = nz(mr.getPaidAmount());
        if (paid.add(amount).compareTo(approved) > 0) {
            throw new BusinessException("Disbursement exceeds remaining approved amount");
        }
        if (disbursementRepository.existsByMaterialRequestIdAndAmount(mr.getId(), amount)) {
            throw new BusinessException("Identical disbursement amount already recorded for this request");
        }

        Long supplierId = body.getSupplierId() != null
                ? body.getSupplierId()
                : (mr.getPreferredSupplier() != null ? mr.getPreferredSupplier().getId() : null);
        if (supplierId == null) {
            throw new BusinessException("supplierId is required for disbursement");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        User me = farmAccessService.currentUser();
        MaterialRequestDisbursement d = new MaterialRequestDisbursement();
        d.setMaterialRequest(mr);
        d.setDisbursementNumber(body.getDisbursementNumber() != null && !body.getDisbursementNumber().isBlank()
                ? body.getDisbursementNumber().trim()
                : numberService.nextMaterialDisbursement());
        d.setSupplier(supplier);
        d.setAmount(amount);
        d.setPaymentMethod(body.getPaymentMethod());
        d.setPaymentDate(body.getPaymentDate() != null ? body.getPaymentDate() : LocalDate.now());
        d.setEvidenceUrl(body.getEvidenceUrl());
        d.setRecordedBy(me);
        d = disbursementRepository.save(d);

        PaymentMethod pm = parsePaymentMethod(body.getPaymentMethod());
        FinanceTransaction tx = financeService.postTransaction(
                TransactionType.EXPENSE,
                amount,
                BusinessUnit.GENERAL,
                AccountCode.EXP_ADMIN.getCode(),
                AccountCode.ASSET_CASH.getCode(),
                "Material disbursement " + d.getDisbursementNumber() + " for " + mr.getRequestNumber(),
                FinanceSourceTypes.MODULE_MATERIALS,
                FinanceSourceTypes.REF_MATERIAL_DISBURSEMENT,
                d.getId(),
                pm,
                d.getPaymentDate());
        d.setFinanceTransactionId(tx.getId());
        disbursementRepository.save(d);

        mr.setPaidAmount(paid.add(amount));
        mr.setLastSupplierPaymentId(d.getId());
        if (mr.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            mr.setStatus(MaterialRequestStatus.PAYMENT_SENT);
        }
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_DISBURSEMENT", AUDIT_MODULE, "MaterialRequestDisbursement", d.getId(),
                null, d.getDisbursementNumber());
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse registerGoodsReceipt(Long id, GoodsReceiptRequest body) {
        MaterialRequest mr = find(id);
        if (mr.getType() != MaterialRequestType.PURCHASE_REQUEST) {
            throw new BusinessException("Goods receipt is only for PURCHASE_REQUEST");
        }
        Set<MaterialRequestStatus> allowed = EnumSet.of(
                MaterialRequestStatus.PURCHASE_AUTHORIZED,
                MaterialRequestStatus.PAYMENT_SENT,
                MaterialRequestStatus.APPROVED,
                MaterialRequestStatus.PARTIALLY_FULFILLED,
                MaterialRequestStatus.GOODS_RECEIVED,
                MaterialRequestStatus.STOCK_REGISTERED
        );
        if (!allowed.contains(mr.getStatus())) {
            throw new BusinessException("Cannot register goods receipt in status " + mr.getStatus());
        }
        if (body == null || body.getLines() == null || body.getLines().isEmpty()) {
            throw new BusinessException("Goods receipt lines are required");
        }

        User me = farmAccessService.currentUser();
        MaterialGoodsReceipt receipt = new MaterialGoodsReceipt();
        receipt.setReceiptNumber(numberService.nextMaterialStoreReceipt());
        receipt.setMaterialRequest(mr);
        receipt.setReceivedBy(me);
        receipt.setReceivedAt(LocalDateTime.now());
        receipt.setNotes(body.getNotes());
        receipt.setPosted(false);
        receipt = goodsReceiptRepository.save(receipt);

        for (GoodsReceiptRequest.LineReceipt lr : body.getLines()) {
            MaterialRequestLine line = resolveLine(mr, lr.getLineId(), lr.getInventoryItemId());
            BigDecimal qty = lr.getQuantityReceived();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Received quantity must be positive");
            }
            BigDecimal cap = remainingReceive(line);
            if (qty.compareTo(cap) > 0) {
                throw new BusinessException("Received quantity exceeds remaining for item "
                        + line.getInventoryItem().getItemName());
            }

            MaterialGoodsReceiptLine gl = new MaterialGoodsReceiptLine();
            gl.setRequestLine(line);
            gl.setInventoryItem(line.getInventoryItem());
            gl.setQuantityReceived(qty.setScale(3, RoundingMode.HALF_UP));
            gl.setBatchNumber(lr.getBatchNumber());
            gl.setExpiryDate(lr.getExpiryDate());
            gl.setConditionNote(lr.getConditionNote());
            receipt.addLine(gl);

            Map<String, Object> stockData = new HashMap<>();
            stockData.put("batchNumber", lr.getBatchNumber() != null ? lr.getBatchNumber()
                    : "MR-" + mr.getRequestNumber() + "-" + line.getId());
            if (lr.getUnitCost() != null) {
                stockData.put("unitCost", lr.getUnitCost());
            } else if (line.getEstimatedUnitPrice() != null) {
                stockData.put("unitCost", line.getEstimatedUnitPrice());
            }
            if (lr.getExpiryDate() != null) {
                stockData.put("expiryDate", lr.getExpiryDate().toString());
            }
            stockData.put("referenceType", FinanceSourceTypes.REF_MATERIAL_RECEIPT);
            stockData.put("referenceId", receipt.getId() * 100_000L + line.getId());

            Map<String, Object> stockResult = inventoryService.stockIn(
                    line.getInventoryItem().getId(),
                    qty.doubleValue(),
                    stockData,
                    me);
            Object batchId = stockResult.get("batchId");
            if (batchId != null) {
                gl.setStockMovementId(Long.valueOf(batchId.toString()));
                line.setLastStockMovementId(gl.getStockMovementId());
            }
            line.setQuantityReceived(nz(line.getQuantityReceived()).add(qty).setScale(3, RoundingMode.HALF_UP));
        }

        receipt.setPosted(true);
        receipt.setStockPostedAt(LocalDateTime.now());
        goodsReceiptRepository.save(receipt);

        updatePurchaseFulfillmentStatus(mr);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_GOODS_RECEIPT", AUDIT_MODULE, "MaterialGoodsReceipt", receipt.getId(),
                null, receipt.getReceiptNumber());
        if (saved.getRequester() != null) {
            notificationService.createUserNotification(
                    "Goods received",
                    saved.getRequestNumber() + " stock registered",
                    saved.getRequester());
        }
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse issueInternal(Long id, IssueRequest body) {
        MaterialRequest mr = find(id);
        if (mr.getType() != MaterialRequestType.INTERNAL_ISSUE_REQUEST) {
            throw new BusinessException("Internal issue is only for INTERNAL_ISSUE_REQUEST");
        }
        Set<MaterialRequestStatus> allowed = EnumSet.of(
                MaterialRequestStatus.APPROVED,
                MaterialRequestStatus.READY_FOR_STORE,
                MaterialRequestStatus.PARTIALLY_FULFILLED
        );
        if (!allowed.contains(mr.getStatus())) {
            throw new BusinessException("Cannot issue in status " + mr.getStatus());
        }
        if (body == null || body.getLines() == null || body.getLines().isEmpty()) {
            throw new BusinessException("Issue lines are required");
        }

        if (body.getIdempotencyKey() != null && !body.getIdempotencyKey().isBlank()) {
            Optional<MaterialIssueEvent> existing = issueEventRepository.findByIdempotencyKey(body.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                return toResponse(mr);
            }
        }

        User me = farmAccessService.currentUser();
        MaterialIssueEvent event = new MaterialIssueEvent();
        event.setIssueNumber(numberService.nextStoreIssue());
        event.setMaterialRequest(mr);
        event.setIssuedBy(me);
        event.setIssuedAt(LocalDateTime.now());
        event.setNotes(body.getNotes());
        event.setIdempotencyKey(body.getIdempotencyKey() != null ? body.getIdempotencyKey().trim() : null);
        event.setPosted(false);
        event = issueEventRepository.save(event);

        for (IssueRequest.LineIssue li : body.getLines()) {
            MaterialRequestLine line = resolveLine(mr, li.getLineId(), null);
            BigDecimal qty = li.getQuantityIssued();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Issued quantity must be positive");
            }
            BigDecimal remaining = remainingIssue(line);
            if (qty.compareTo(remaining) > 0) {
                throw new BusinessException("Issued quantity exceeds remaining approved for item "
                        + line.getInventoryItem().getItemName());
            }

            Map<String, Object> stockResult = inventoryService.stockOut(
                    line.getInventoryItem().getId(),
                    qty.doubleValue(),
                    "MATERIAL_ISSUE",
                    mr.getId(),
                    me,
                    null,
                    FinanceSourceTypes.REF_MATERIAL_ISSUE,
                    "INTERNAL_CONSUMPTION");

            MaterialIssueEventLine el = new MaterialIssueEventLine();
            el.setRequestLine(line);
            el.setQuantityIssued(qty.setScale(3, RoundingMode.HALF_UP));
            Object batchId = stockResult.get("batchId");
            if (batchId != null) {
                el.setStockMovementId(Long.valueOf(batchId.toString()));
                line.setLastStockMovementId(el.getStockMovementId());
            }
            event.addLine(el);
            line.setQuantityIssued(nz(line.getQuantityIssued()).add(qty).setScale(3, RoundingMode.HALF_UP));

            BigDecimal cost = BigDecimal.ZERO;
            Object totalCostObj = stockResult.get("totalCost");
            if (totalCostObj instanceof BigDecimal bd) {
                cost = bd;
            } else if (totalCostObj != null) {
                cost = new BigDecimal(totalCostObj.toString());
            } else {
                BigDecimal unitCost = line.getInventoryItem().getAvgUnitCost() != null
                        ? line.getInventoryItem().getAvgUnitCost()
                        : (line.getEstimatedUnitPrice() != null ? line.getEstimatedUnitPrice() : BigDecimal.ZERO);
                cost = unitCost.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            }
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                financeService.postInventoryConsumption(
                        cost,
                        BusinessUnit.GENERAL,
                        AccountCode.EXP_ADMIN.getCode(),
                        "Material issue " + event.getIssueNumber() + " line " + line.getId(),
                        FinanceSourceTypes.MODULE_MATERIALS,
                        FinanceSourceTypes.REF_MATERIAL_ISSUE,
                        event.getId() * 100_000L + line.getId());
            }
        }

        event.setPosted(true);
        issueEventRepository.save(event);
        updateInternalFulfillmentStatus(mr);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_ISSUED", AUDIT_MODULE, "MaterialIssueEvent", event.getId(), null, event.getIssueNumber());
        if (saved.getRequester() != null) {
            notificationService.createUserNotification(
                    "Materials issued",
                    saved.getRequestNumber() + " issued from store",
                    saved.getRequester());
        }
        notificationService.createRoleNotification(
                "Materials issued",
                saved.getRequestNumber() + " awaiting acknowledgement",
                Role.FARM_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse acknowledgeReceipt(Long id, AcknowledgeRequest body) {
        MaterialRequest mr = find(id);
        if (mr.getStatus() != MaterialRequestStatus.ISSUED
                && mr.getStatus() != MaterialRequestStatus.PARTIALLY_FULFILLED
                && mr.getStatus() != MaterialRequestStatus.FULFILLED) {
            throw new BusinessException("Can only acknowledge issued requests");
        }
        User me = farmAccessService.currentUser();
        if (mr.getFarm() != null) {
            farmAccessService.assertCanAccessFarm(me, mr.getFarm().getId());
        }
        String old = mr.getStatus().name();
        mr.setAcknowledgedBy(me);
        mr.setAcknowledgedAt(LocalDateTime.now());
        mr.setAcknowledgementNotes(body != null ? body.getNotes() : null);
        mr.setStatus(MaterialRequestStatus.ACKNOWLEDGED);
        if (allLinesFullyIssued(mr)) {
            mr.setStatus(MaterialRequestStatus.FULFILLED);
        }
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_ACKNOWLEDGED", AUDIT_MODULE, "MaterialRequest", saved.getId(), old, saved.getStatus().name());
        notificationService.createRoleNotification(
                "Material receipt acknowledged",
                saved.getRequestNumber() + " acknowledged by farm",
                Role.INVENTORY_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public MaterialRequestResponse cancel(Long id) {
        MaterialRequest mr = find(id);
        if (mr.getStatus() == MaterialRequestStatus.FULFILLED
                || mr.getStatus() == MaterialRequestStatus.ACKNOWLEDGED) {
            throw new BusinessException("Cannot cancel a fulfilled request");
        }
        boolean hasStock = mr.getLines().stream().anyMatch(l ->
                nz(l.getQuantityReceived()).compareTo(BigDecimal.ZERO) > 0
                        || nz(l.getQuantityIssued()).compareTo(BigDecimal.ZERO) > 0);
        if (hasStock || STOCK_POSTED_STATUSES.contains(mr.getStatus())) {
            throw new BusinessException("Cannot cancel after stock has been posted");
        }
        String old = mr.getStatus().name();
        mr.setStatus(MaterialRequestStatus.CANCELLED);
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log("MR_CANCELLED", AUDIT_MODULE, "MaterialRequest", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("financePending", requestRepository.countByStatusIn(List.of(
                MaterialRequestStatus.SUBMITTED, MaterialRequestStatus.FINANCE_REVIEW)));
        m.put("storePendingIssues", requestRepository.countByTypeAndStatusIn(
                MaterialRequestType.INTERNAL_ISSUE_REQUEST,
                List.of(MaterialRequestStatus.READY_FOR_STORE, MaterialRequestStatus.PARTIALLY_FULFILLED)));
        m.put("pendingReceipts", requestRepository.countByTypeAndStatusIn(
                MaterialRequestType.PURCHASE_REQUEST,
                List.of(MaterialRequestStatus.PURCHASE_AUTHORIZED, MaterialRequestStatus.PAYMENT_SENT,
                        MaterialRequestStatus.PARTIALLY_FULFILLED)));
        long total = requestRepository.count();
        long fulfilled = requestRepository.countByStatusIn(List.of(
                MaterialRequestStatus.FULFILLED, MaterialRequestStatus.ACKNOWLEDGED));
        m.put("totalRequests", total);
        m.put("fulfilled", fulfilled);
        m.put("fulfillmentRate", total == 0 ? 0.0 : (fulfilled * 100.0 / total));
        return m;
    }

    private MaterialRequestResponse financeDecision(Long id, MaterialRequestStatus target,
                                                    FinanceDecisionRequest body, String auditAction) {
        MaterialRequest mr = find(id);
        User me = farmAccessService.currentUser();
        if (mr.getStatus() != MaterialRequestStatus.FINANCE_REVIEW
                && mr.getStatus() != MaterialRequestStatus.SUBMITTED) {
            throw new BusinessException("Request is not awaiting finance decision");
        }
        if (body == null || body.getReason() == null || body.getReason().isBlank()) {
            throw new BusinessException("Reason is required");
        }
        if (mr.getRequester() != null && mr.getRequester().getId().equals(me.getId())) {
            throw new BusinessException("Cannot decide on your own material request (SoD)");
        }
        String old = mr.getStatus().name();
        mr.setStatus(target);
        mr.setRejectionReason(body.getReason().trim());
        MaterialRequest saved = requestRepository.save(mr);
        auditService.log(auditAction, AUDIT_MODULE, "MaterialRequest", saved.getId(), old, target.name());
        if (saved.getRequester() != null) {
            notificationService.createUserNotification(
                    "Material request " + target.name().toLowerCase(),
                    saved.getRequestNumber() + ": " + saved.getRejectionReason(),
                    saved.getRequester());
        }
        return toResponse(saved);
    }

    private void applyFields(MaterialRequest mr, MaterialRequestCreateRequest req) {
        MaterialRequestType type = parseType(req.getType() != null ? req.getType() : mr.getType().name());
        mr.setType(type);
        mr.setDepartment(req.getDepartment());
        mr.setNeededBy(req.getNeededBy());
        mr.setPriority(req.getPriority() != null ? req.getPriority().toUpperCase() : "NORMAL");
        mr.setReason(req.getReason());
        mr.setNotes(req.getNotes());
        mr.setAttachmentUrl(req.getAttachmentUrl());
        if (req.getRequestDate() != null) {
            mr.setRequestDate(req.getRequestDate());
        }

        if (req.getFarmId() != null) {
            Farm farm = farmRepository.findById(req.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
            User me = farmAccessService.currentUser();
            farmAccessService.assertCanAccessFarm(me, farm.getId());
            mr.setFarm(farm);
        } else if (type == MaterialRequestType.INTERNAL_ISSUE_REQUEST) {
            throw new BusinessException("Farm is required for internal issue requests");
        } else {
            mr.setFarm(null);
        }

        if (req.getPreferredSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(req.getPreferredSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
            mr.setPreferredSupplier(supplier);
        } else {
            mr.setPreferredSupplier(null);
        }

        mr.clearLines();
        BigDecimal total = BigDecimal.ZERO;
        for (MaterialRequestCreateRequest.LineRequest lr : req.getLines()) {
            if (lr.getQuantityRequested() == null || lr.getQuantityRequested().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Line quantity must be greater than zero");
            }
            InventoryItem item = itemRepository.findById(lr.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + lr.getInventoryItemId()));
            MaterialRequestLine line = new MaterialRequestLine();
            line.setInventoryItem(item);
            line.setQuantityRequested(lr.getQuantityRequested().setScale(3, RoundingMode.HALF_UP));
            line.setQuantityApproved(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            line.setQuantityOrdered(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            line.setQuantityReceived(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            line.setQuantityIssued(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            line.setUnit(lr.getUnit() != null ? lr.getUnit()
                    : (item.getUnit() != null ? item.getUnit().name() : null));
            line.setEstimatedUnitPrice(lr.getEstimatedUnitPrice());
            if (lr.getEstimatedUnitPrice() != null) {
                line.setEstimatedLineTotal(lr.getEstimatedUnitPrice()
                        .multiply(line.getQuantityRequested()).setScale(2, RoundingMode.HALF_UP));
                total = total.add(line.getEstimatedLineTotal());
            }
            line.setPurpose(lr.getPurpose());
            line.setNotes(lr.getNotes());
            mr.addLine(line);
        }
        mr.setEstimatedTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private void assertNoDuplicate(MaterialRequest mr) {
        Long farmId = mr.getFarm() != null ? mr.getFarm().getId() : null;
        String purpose = mr.getReason() != null ? mr.getReason() : "";
        for (MaterialRequestLine line : mr.getLines()) {
            List<MaterialRequest> dupes = requestRepository.findOpenDuplicates(
                    mr.getRequester().getId(),
                    farmId,
                    line.getInventoryItem().getId(),
                    purpose,
                    OPEN_DUPLICATE_STATUSES);
            for (MaterialRequest d : dupes) {
                if (!d.getId().equals(mr.getId())) {
                    throw new BusinessException(
                            "Duplicate pending material request exists: " + d.getRequestNumber()
                                    + " for same requester/item/farm/purpose");
                }
            }
        }
    }

    private void updatePurchaseFulfillmentStatus(MaterialRequest mr) {
        boolean any = false;
        boolean all = true;
        for (MaterialRequestLine line : mr.getLines()) {
            BigDecimal target = nz(line.getQuantityOrdered()).compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityOrdered()
                    : (nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityApproved() : line.getQuantityRequested());
            BigDecimal received = nz(line.getQuantityReceived());
            if (received.compareTo(BigDecimal.ZERO) > 0) {
                any = true;
            }
            if (received.compareTo(target) < 0) {
                all = false;
            }
        }
        if (all && any) {
            mr.setStatus(MaterialRequestStatus.FULFILLED);
        } else if (any && !all) {
            mr.setStatus(MaterialRequestStatus.PARTIALLY_FULFILLED);
        } else if (any) {
            mr.setStatus(MaterialRequestStatus.STOCK_REGISTERED);
        }
    }

    private void updateInternalFulfillmentStatus(MaterialRequest mr) {
        boolean any = false;
        boolean all = true;
        for (MaterialRequestLine line : mr.getLines()) {
            BigDecimal target = nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityApproved() : line.getQuantityRequested();
            BigDecimal issued = nz(line.getQuantityIssued());
            if (issued.compareTo(BigDecimal.ZERO) > 0) {
                any = true;
            }
            if (issued.compareTo(target) < 0) {
                all = false;
            }
        }
        if (all && any) {
            mr.setStatus(MaterialRequestStatus.ISSUED);
        } else if (any) {
            mr.setStatus(MaterialRequestStatus.PARTIALLY_FULFILLED);
        }
    }

    private boolean allLinesFullyIssued(MaterialRequest mr) {
        for (MaterialRequestLine line : mr.getLines()) {
            BigDecimal target = nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                    ? line.getQuantityApproved() : line.getQuantityRequested();
            if (nz(line.getQuantityIssued()).compareTo(target) < 0) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal remainingReceive(MaterialRequestLine line) {
        BigDecimal target = nz(line.getQuantityOrdered()).compareTo(BigDecimal.ZERO) > 0
                ? line.getQuantityOrdered()
                : (nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                ? line.getQuantityApproved() : line.getQuantityRequested());
        return target.subtract(nz(line.getQuantityReceived())).max(BigDecimal.ZERO);
    }

    private BigDecimal remainingIssue(MaterialRequestLine line) {
        BigDecimal target = nz(line.getQuantityApproved()).compareTo(BigDecimal.ZERO) > 0
                ? line.getQuantityApproved() : line.getQuantityRequested();
        return target.subtract(nz(line.getQuantityIssued())).max(BigDecimal.ZERO);
    }

    private MaterialRequestLine resolveLine(MaterialRequest mr, Long lineId, Long itemId) {
        if (lineId != null) {
            return mr.getLines().stream()
                    .filter(l -> l.getId().equals(lineId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Request line not found: " + lineId));
        }
        if (itemId != null) {
            return mr.getLines().stream()
                    .filter(l -> l.getInventoryItem().getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Request line for item not found: " + itemId));
        }
        throw new BusinessException("lineId or inventoryItemId is required");
    }

    private MaterialRequestType parseType(String type) {
        try {
            return MaterialRequestType.valueOf(type.trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException("Invalid material request type: " + type);
        }
    }

    private PaymentMethod parsePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return PaymentMethod.CASH;
        }
        try {
            return PaymentMethod.valueOf(method.trim().toUpperCase());
        } catch (Exception e) {
            return PaymentMethod.CASH;
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private MaterialRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material request not found: " + id));
    }

    MaterialRequestResponse toResponse(MaterialRequest mr) {
        MaterialRequestResponse r = new MaterialRequestResponse();
        r.setId(mr.getId());
        r.setRequestNumber(mr.getRequestNumber());
        r.setType(mr.getType().name());
        r.setStatus(mr.getStatus().name());
        if (mr.getRequester() != null) {
            r.setRequesterId(mr.getRequester().getId());
            r.setRequesterName(mr.getRequester().getFullName());
        }
        r.setDepartment(mr.getDepartment());
        if (mr.getFarm() != null) {
            r.setFarmId(mr.getFarm().getId());
            r.setFarmName(mr.getFarm().getFarmName());
        }
        r.setRequestDate(mr.getRequestDate());
        r.setNeededBy(mr.getNeededBy());
        r.setPriority(mr.getPriority());
        r.setReason(mr.getReason());
        r.setNotes(mr.getNotes());
        r.setAttachmentUrl(mr.getAttachmentUrl());
        if (mr.getPreferredSupplier() != null) {
            r.setPreferredSupplierId(mr.getPreferredSupplier().getId());
            r.setPreferredSupplierName(mr.getPreferredSupplier().getName());
        }
        r.setEstimatedTotal(mr.getEstimatedTotal());
        r.setApprovedAmount(mr.getApprovedAmount());
        r.setPaidAmount(mr.getPaidAmount());
        r.setRejectionReason(mr.getRejectionReason());
        if (mr.getFinanceApprovedBy() != null) {
            r.setFinanceApprovedById(mr.getFinanceApprovedBy().getId());
            r.setFinanceApprovedByName(mr.getFinanceApprovedBy().getFullName());
        }
        r.setFinanceApprovedAt(mr.getFinanceApprovedAt());
        r.setProcRequisitionId(mr.getProcRequisitionId());
        r.setInternalRequisitionId(mr.getInternalRequisitionId());
        r.setPurchaseOrderId(mr.getPurchaseOrderId());
        r.setLastSupplierPaymentId(mr.getLastSupplierPaymentId());
        if (mr.getAcknowledgedBy() != null) {
            r.setAcknowledgedById(mr.getAcknowledgedBy().getId());
            r.setAcknowledgedByName(mr.getAcknowledgedBy().getFullName());
        }
        r.setAcknowledgedAt(mr.getAcknowledgedAt());
        r.setAcknowledgementNotes(mr.getAcknowledgementNotes());
        r.setCreatedAt(mr.getCreatedAt());
        r.setUpdatedAt(mr.getUpdatedAt());
        for (MaterialRequestLine line : mr.getLines()) {
            MaterialRequestResponse.LineResponse lr = new MaterialRequestResponse.LineResponse();
            lr.setId(line.getId());
            if (line.getInventoryItem() != null) {
                lr.setInventoryItemId(line.getInventoryItem().getId());
                lr.setItemName(line.getInventoryItem().getItemName());
                lr.setSku(line.getInventoryItem().getSku());
            }
            lr.setQuantityRequested(line.getQuantityRequested());
            lr.setQuantityApproved(line.getQuantityApproved());
            lr.setQuantityOrdered(line.getQuantityOrdered());
            lr.setQuantityReceived(line.getQuantityReceived());
            lr.setQuantityIssued(line.getQuantityIssued());
            lr.setUnit(line.getUnit());
            lr.setEstimatedUnitPrice(line.getEstimatedUnitPrice());
            lr.setEstimatedLineTotal(line.getEstimatedLineTotal());
            lr.setPurpose(line.getPurpose());
            lr.setNotes(line.getNotes());
            lr.setLastStockMovementId(line.getLastStockMovementId());
            r.getLines().add(lr);
        }
        return r;
    }
}
