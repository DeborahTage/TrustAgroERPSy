package com.trustagro.procurement.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.*;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.dto.*;
import com.trustagro.procurement.entity.*;
import com.trustagro.procurement.repository.*;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class ProcurementService {

    public static final String MODULE = "PROCUREMENT";
    private static final Set<String> PAYMENT_TERMS = Set.of("CASH", "NET_7", "NET_15", "NET_30", "NET_60");

    private final ProcNumberService numberService;
    private final ProcRequisitionRepository requisitionRepository;
    private final ProcRequisitionItemRepository requisitionItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierInvoiceItemRepository supplierInvoiceItemRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;
    private final FinanceService financeService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ─── Requisitions ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RequisitionResponse> listRequisitions(ProcRequisitionStatus status) {
        List<ProcRequisition> list = status != null
                ? requisitionRepository.findByStatusOrderByCreatedAtDesc(status)
                : requisitionRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toRequisitionResponse).toList();
    }

    @Transactional(readOnly = true)
    public RequisitionResponse getRequisition(Long id) {
        return toRequisitionResponse(requireRequisition(id));
    }

    @Transactional
    public RequisitionResponse createRequisition(RequisitionRequest req) {
        User me = requireCurrentUser();
        ProcRequisition r = new ProcRequisition();
        r.setRequisitionNumber(numberService.nextRequisitionNumber());
        r.setStatus(ProcRequisitionStatus.DRAFT);
        applyRequisitionFields(r, req);
        r.setRequestedBy(me);
        r = requisitionRepository.save(r);
        saveRequisitionItems(r, req.getItems());
        auditService.log("PROC_REQUISITION_CREATED", MODULE, "ProcRequisition", r.getId(), null, r.getRequisitionNumber());
        return toRequisitionResponse(r);
    }

    @Transactional
    public RequisitionResponse updateRequisition(Long id, RequisitionRequest req) {
        ProcRequisition r = requireRequisition(id);
        if (r.getStatus() != ProcRequisitionStatus.DRAFT) {
            throw new BusinessException("Only DRAFT requisitions can be updated");
        }
        applyRequisitionFields(r, req);
        requisitionItemRepository.deleteByRequisitionId(r.getId());
        saveRequisitionItems(r, req.getItems());
        r = requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_UPDATED", MODULE, "ProcRequisition", r.getId(), null, r.getRequisitionNumber());
        return toRequisitionResponse(r);
    }

    @Transactional
    public RequisitionResponse submitRequisition(Long id) {
        ProcRequisition r = requireRequisition(id);
        ProcRequisitionLifecycle.assertTransition(r.getStatus(), ProcRequisitionStatus.SUBMITTED);
        if (requisitionItemRepository.findByRequisitionIdOrderByIdAsc(r.getId()).isEmpty()) {
            throw new BusinessException("Requisition has no line items");
        }
        r.setStatus(ProcRequisitionStatus.SUBMITTED);
        r = requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_SUBMITTED", MODULE, "ProcRequisition", r.getId(), null, r.getStatus().name());
        notificationService.createRoleNotification("Requisition submitted",
                r.getRequisitionNumber() + " awaiting review", Role.INVENTORY_MANAGER);
        return toRequisitionResponse(r);
    }

    @Transactional
    public RequisitionResponse reviewRequisition(Long id) {
        ProcRequisition r = requireRequisition(id);
        ProcRequisitionLifecycle.assertTransition(r.getStatus(), ProcRequisitionStatus.UNDER_REVIEW);
        r.setStatus(ProcRequisitionStatus.UNDER_REVIEW);
        r.setReviewedBy(requireCurrentUser());
        r.setReviewedAt(LocalDateTime.now());
        return toRequisitionResponse(requisitionRepository.save(r));
    }

    @Transactional
    public RequisitionResponse approveRequisition(Long id) {
        ProcRequisition r = requireRequisition(id);
        User me = requireCurrentUser();
        if (r.getRequestedBy() != null && r.getRequestedBy().getId().equals(me.getId())) {
            throw new BusinessException("Cannot self-approve a requisition");
        }
        ProcRequisitionStatus target = ProcRequisitionStatus.APPROVED;
        if (r.getStatus() == ProcRequisitionStatus.SUBMITTED) {
            ProcRequisitionLifecycle.assertTransition(ProcRequisitionStatus.SUBMITTED, target);
        } else {
            ProcRequisitionLifecycle.assertTransition(r.getStatus(), target);
        }
        r.setStatus(target);
        r.setApprovedBy(me);
        r.setApprovedAt(LocalDateTime.now());
        r = requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_APPROVED", MODULE, "ProcRequisition", r.getId(), null, me.getEmail());
        return toRequisitionResponse(r);
    }

    @Transactional
    public RequisitionResponse rejectRequisition(Long id, DecisionRequest body) {
        ProcRequisition r = requireRequisition(id);
        User me = requireCurrentUser();
        if (r.getRequestedBy() != null && r.getRequestedBy().getId().equals(me.getId())) {
            throw new BusinessException("Cannot self-reject a requisition");
        }
        ProcRequisitionLifecycle.assertTransition(r.getStatus(), ProcRequisitionStatus.REJECTED);
        r.setStatus(ProcRequisitionStatus.REJECTED);
        r.setRejectionReason(body != null ? body.getReason() : null);
        r.setApprovedBy(me);
        r.setApprovedAt(LocalDateTime.now());
        r = requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_REJECTED", MODULE, "ProcRequisition", r.getId(), null, r.getRejectionReason());
        return toRequisitionResponse(r);
    }

    @Transactional
    public RequisitionResponse cancelRequisition(Long id) {
        ProcRequisition r = requireRequisition(id);
        ProcRequisitionLifecycle.assertTransition(r.getStatus(), ProcRequisitionStatus.CANCELLED);
        r.setStatus(ProcRequisitionStatus.CANCELLED);
        r = requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_CANCELLED", MODULE, "ProcRequisition", r.getId(), null, null);
        return toRequisitionResponse(r);
    }

    @Transactional
    public PurchaseOrderResponse convertRequisitionToPo(Long id, Long supplierId) {
        ProcRequisition r = requireRequisition(id);
        if (r.getStatus() == ProcRequisitionStatus.CONVERTED && r.getConvertedPurchaseOrder() != null) {
            return toPurchaseOrderResponse(r.getConvertedPurchaseOrder());
        }
        ProcRequisitionLifecycle.assertTransition(r.getStatus(), ProcRequisitionStatus.CONVERTED);
        if (supplierId == null) {
            throw new BusinessException("supplierId is required to convert requisition to PO");
        }
        Supplier supplier = requireSupplier(supplierId);
        List<ProcRequisitionItem> lines = requisitionItemRepository.findByRequisitionIdOrderByIdAsc(r.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("Requisition has no line items");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(numberService.nextPoNumber());
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setOrderDate(LocalDate.now());
        po.setPaymentTerms(normalizePaymentTerms(supplier.getPaymentTerms()));
        po.setCurrency(supplier.getCurrency() != null ? supplier.getCurrency() : "ETB");
        po.setRequisition(r);
        po.setCreatedBy(requireCurrentUser());
        po.setNotes("Converted from " + r.getRequisitionNumber());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PurchaseOrderItem> poItems = new ArrayList<>();
        for (ProcRequisitionItem line : lines) {
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPurchaseOrder(po);
            poi.setItem(line.getItem());
            poi.setQuantityOrdered(line.getQuantityRequested());
            BigDecimal price = line.getEstimatedUnitPrice() != null
                    ? line.getEstimatedUnitPrice()
                    : (line.getItem().getLastPurchasePrice() != null
                        ? line.getItem().getLastPurchasePrice() : BigDecimal.ZERO);
            poi.setUnitPrice(scale(price));
            poi.setLineTotal(scale(price.multiply(BigDecimal.valueOf(line.getQuantityRequested()))));
            poi.setNotes(line.getNotes());
            subtotal = subtotal.add(poi.getLineTotal());
            poItems.add(poi);
        }
        po.setSubtotal(scale(subtotal));
        po.setTaxAmount(BigDecimal.ZERO);
        po.setTotalAmount(scale(subtotal));
        po = purchaseOrderRepository.save(po);
        for (PurchaseOrderItem poi : poItems) {
            poi.setPurchaseOrder(po);
            purchaseOrderItemRepository.save(poi);
        }

        r.setStatus(ProcRequisitionStatus.CONVERTED);
        r.setConvertedPurchaseOrder(po);
        requisitionRepository.save(r);
        auditService.log("PROC_REQUISITION_CONVERTED", MODULE, "ProcRequisition", r.getId(),
                r.getRequisitionNumber(), po.getPoNumber());
        return toPurchaseOrderResponse(po);
    }

    // ─── Purchase Orders ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> listPurchaseOrders(PurchaseOrderStatus status) {
        return purchaseOrderRepository.findAll().stream()
                .filter(po -> status == null || po.getStatus() == status)
                .sorted(Comparator.comparing(PurchaseOrder::getId).reversed())
                .map(this::toPurchaseOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrder(Long id) {
        return toPurchaseOrderResponse(requirePurchaseOrder(id));
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest req) {
        Supplier supplier = requireSupplier(req.getSupplierId());
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(numberService.nextPoNumber());
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setCreatedBy(requireCurrentUser());
        applyPoFields(po, req, supplier);
        po = purchaseOrderRepository.save(po);
        savePoItems(po, req.getItems());
        recalculatePoTotals(po, req.getTaxAmount());
        po = purchaseOrderRepository.save(po);
        auditService.log("PROC_PO_CREATED", MODULE, "PurchaseOrder", po.getId(), null, po.getPoNumber());
        return toPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(Long id, PurchaseOrderRequest req) {
        PurchaseOrder po = requirePurchaseOrder(id);
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be updated");
        }
        Supplier supplier = requireSupplier(req.getSupplierId());
        applyPoFields(po, req, supplier);
        List<PurchaseOrderItem> existing = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
        purchaseOrderItemRepository.deleteAll(existing);
        savePoItems(po, req.getItems());
        recalculatePoTotals(po, req.getTaxAmount());
        po = purchaseOrderRepository.save(po);
        auditService.log("PROC_PO_UPDATED", MODULE, "PurchaseOrder", po.getId(), null, po.getPoNumber());
        return toPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse submitPurchaseOrder(Long id) {
        PurchaseOrder po = requirePurchaseOrder(id);
        PurchaseOrderLifecycle.assertTransition(po.getStatus(), PurchaseOrderStatus.SUBMITTED);
        ensurePoHasLines(po);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        return toPurchaseOrderResponse(purchaseOrderRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponse approvePurchaseOrder(Long id) {
        PurchaseOrder po = requirePurchaseOrder(id);
        PurchaseOrderLifecycle.assertTransition(po.getStatus(), PurchaseOrderStatus.APPROVED);
        User me = requireCurrentUser();
        if (po.getCreatedBy() != null && po.getCreatedBy().getId().equals(me.getId())) {
            throw new BusinessException("Cannot self-approve a purchase order");
        }
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(me);
        po.setApprovedAt(LocalDateTime.now());
        po = purchaseOrderRepository.save(po);
        auditService.log("PROC_PO_APPROVED", MODULE, "PurchaseOrder", po.getId(), null, me.getEmail());
        notificationService.createRoleNotification("PO approved",
                po.getPoNumber() + " approved", Role.INVENTORY_MANAGER);
        return toPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse sendPurchaseOrder(Long id) {
        PurchaseOrder po = requirePurchaseOrder(id);
        PurchaseOrderLifecycle.assertTransition(po.getStatus(), PurchaseOrderStatus.SENT);
        po.setStatus(PurchaseOrderStatus.SENT);
        po = purchaseOrderRepository.save(po);
        auditService.log("PROC_PO_SENT", MODULE, "PurchaseOrder", po.getId(), null, po.getPoNumber());
        return toPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long id) {
        PurchaseOrder po = requirePurchaseOrder(id);
        PurchaseOrderLifecycle.assertTransition(po.getStatus(), PurchaseOrderStatus.CANCELLED);
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po = purchaseOrderRepository.save(po);
        auditService.log("PROC_PO_CANCELLED", MODULE, "PurchaseOrder", po.getId(), null, null);
        return toPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse closePurchaseOrder(Long id) {
        PurchaseOrder po = requirePurchaseOrder(id);
        PurchaseOrderLifecycle.assertTransition(po.getStatus(), PurchaseOrderStatus.CLOSED);
        po.setStatus(PurchaseOrderStatus.CLOSED);
        return toPurchaseOrderResponse(purchaseOrderRepository.save(po));
    }

    // ─── Goods Receipts ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> listGoodsReceipts(GoodsReceiptStatus status) {
        List<GoodsReceipt> list = status != null
                ? goodsReceiptRepository.findByStatusOrderByCreatedAtDesc(status)
                : goodsReceiptRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toGoodsReceiptResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getGoodsReceipt(Long id) {
        return toGoodsReceiptResponse(requireGoodsReceipt(id));
    }

    @Transactional
    public GoodsReceiptResponse createGoodsReceipt(GoodsReceiptRequest req) {
        PurchaseOrder po = requirePurchaseOrder(req.getPurchaseOrderId());
        if (po.getStatus() != PurchaseOrderStatus.SENT
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED
                && po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new BusinessException("GRN allowed only when PO is APPROVED, SENT, or PARTIALLY_RECEIVED");
        }
        GoodsReceipt grn = new GoodsReceipt();
        grn.setGrnNumber(numberService.nextGrnNumber());
        grn.setPurchaseOrder(po);
        grn.setSupplier(po.getSupplier());
        grn.setStatus(GoodsReceiptStatus.DRAFT);
        grn.setReceiptDate(req.getReceiptDate() != null ? req.getReceiptDate() : LocalDate.now());
        grn.setNotes(req.getNotes());
        grn.setReceivedBy(requireCurrentUser());
        grn = goodsReceiptRepository.save(grn);

        for (GoodsReceiptRequest.Line line : req.getItems()) {
            PurchaseOrderItem poi = requirePoItem(line.getPurchaseOrderItemId());
            if (!poi.getPurchaseOrder().getId().equals(po.getId())) {
                throw new BusinessException("PO line " + poi.getId() + " does not belong to PO " + po.getId());
            }
            double received = line.getReceivedQty() != null ? line.getReceivedQty() : 0.0;
            if (received <= 0) {
                throw new BusinessException("receivedQty must be positive");
            }
            double remaining = remainingQty(poi);
            if (received > remaining + 1e-9) {
                throw new BusinessException("Cannot receive more than remaining qty on PO line "
                        + poi.getId() + " (remaining=" + remaining + ")");
            }
            GoodsReceiptItem gri = new GoodsReceiptItem();
            gri.setGoodsReceipt(grn);
            gri.setPurchaseOrderItem(poi);
            gri.setItem(poi.getItem());
            gri.setReceivedQty(received);
            gri.setRejectedQty(line.getRejectedQty() != null ? line.getRejectedQty() : 0.0);
            gri.setBatchNumber(line.getBatchNumber());
            gri.setExpiryDate(line.getExpiryDate());
            gri.setManufactureDate(line.getManufactureDate());
            gri.setUnitCost(line.getUnitCost() != null ? scale(line.getUnitCost())
                    : (poi.getUnitPrice() != null ? scale(poi.getUnitPrice()) : null));
            goodsReceiptItemRepository.save(gri);
        }
        auditService.log("PROC_GRN_CREATED", MODULE, "GoodsReceipt", grn.getId(), null, grn.getGrnNumber());
        return toGoodsReceiptResponse(grn);
    }

    @Transactional
    public GoodsReceiptResponse postGoodsReceipt(Long id) {
        GoodsReceipt grn = requireGoodsReceipt(id);
        if (grn.getStatus() == GoodsReceiptStatus.POSTED) {
            return toGoodsReceiptResponse(grn);
        }
        GoodsReceiptLifecycle.assertTransition(grn.getStatus(), GoodsReceiptStatus.POSTED);
        PurchaseOrder po = grn.getPurchaseOrder();
        User me = requireCurrentUser();
        List<GoodsReceiptItem> lines = goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(grn.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("GRN has no lines to post");
        }

        for (GoodsReceiptItem line : lines) {
            PurchaseOrderItem poi = line.getPurchaseOrderItem();
            double remaining = remainingQty(poi);
            if (line.getReceivedQty() > remaining + 1e-9) {
                throw new BusinessException("Cannot post: qty exceeds remaining on PO line " + poi.getId());
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("batchNumber", line.getBatchNumber() != null && !line.getBatchNumber().isBlank()
                    ? line.getBatchNumber()
                    : grn.getGrnNumber() + "-" + line.getId());
            if (line.getUnitCost() != null) {
                data.put("unitCost", line.getUnitCost().toPlainString());
            }
            if (line.getExpiryDate() != null) {
                data.put("expiryDate", line.getExpiryDate().toString());
            }
            if (line.getManufactureDate() != null) {
                data.put("manufactureDate", line.getManufactureDate().toString());
            }
            if (grn.getSupplier() != null) {
                data.put("supplierId", grn.getSupplier().getId());
            }
            data.put("poReference", po.getPoNumber());
            data.put("grnId", grn.getId());
            data.put("referenceType", FinanceSourceTypes.REF_PROCUREMENT_GRN);
            data.put("referenceId", grn.getId());

            Map<String, Object> stockResult = inventoryService.stockIn(
                    line.getItem().getId(), line.getReceivedQty(), data, me);
            Object batchId = stockResult.get("batchId");
            if (batchId != null) {
                line.setStockBatchId(Long.valueOf(batchId.toString()));
                goodsReceiptItemRepository.save(line);
            }

            double newReceived = poi.getQuantityReceived() + line.getReceivedQty();
            poi.setQuantityReceived(newReceived);
            purchaseOrderItemRepository.save(poi);
        }

        refreshPoReceiptStatus(po);
        grn.setStatus(GoodsReceiptStatus.POSTED);
        grn.setPostedAt(LocalDateTime.now());
        if (grn.getReceivedBy() == null) {
            grn.setReceivedBy(me);
        }
        grn = goodsReceiptRepository.save(grn);

        auditService.log("GRN_POSTED", MODULE, "GoodsReceipt", grn.getId(), null, grn.getGrnNumber());
        notificationService.createRoleNotification("GRN posted",
                grn.getGrnNumber() + " posted against " + po.getPoNumber(), Role.FINANCE);
        return toGoodsReceiptResponse(grn);
    }

    @Transactional
    public GoodsReceiptResponse cancelGoodsReceipt(Long id) {
        GoodsReceipt grn = requireGoodsReceipt(id);
        GoodsReceiptLifecycle.assertTransition(grn.getStatus(), GoodsReceiptStatus.CANCELLED);
        grn.setStatus(GoodsReceiptStatus.CANCELLED);
        grn = goodsReceiptRepository.save(grn);
        auditService.log("PROC_GRN_CANCELLED", MODULE, "GoodsReceipt", grn.getId(), null, null);
        return toGoodsReceiptResponse(grn);
    }

    // ─── Supplier Invoices (3-way match, no AP accrual) ──────────

    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> listSupplierInvoices(SupplierInvoiceStatus status) {
        List<SupplierInvoice> list = status != null
                ? supplierInvoiceRepository.findByStatusOrderByCreatedAtDesc(status)
                : supplierInvoiceRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toSupplierInvoiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getSupplierInvoice(Long id) {
        return toSupplierInvoiceResponse(requireSupplierInvoice(id));
    }

    @Transactional
    public SupplierInvoiceResponse createSupplierInvoice(SupplierInvoiceRequest req) {
        Supplier supplier = requireSupplier(req.getSupplierId());
        if (supplierInvoiceRepository.existsBySupplierIdAndSupplierInvoiceNumber(
                supplier.getId(), req.getSupplierInvoiceNumber().trim())) {
            throw new BusinessException("Supplier invoice number already exists for this supplier");
        }
        SupplierInvoice inv = new SupplierInvoice();
        inv.setInvoiceNumber(numberService.nextSupplierInvoiceNumber());
        inv.setSupplierInvoiceNumber(req.getSupplierInvoiceNumber().trim());
        inv.setSupplier(supplier);
        inv.setStatus(SupplierInvoiceStatus.DRAFT);
        inv.setMatchStatus(MatchStatus.PENDING);
        inv.setInvoiceDate(req.getInvoiceDate() != null ? req.getInvoiceDate() : LocalDate.now());
        inv.setDueDate(req.getDueDate());
        inv.setCurrency(req.getCurrency() != null ? req.getCurrency()
                : (supplier.getCurrency() != null ? supplier.getCurrency() : "ETB"));
        inv.setNotes(req.getNotes());
        inv.setCreatedBy(requireCurrentUser());
        if (req.getPurchaseOrderId() != null) {
            inv.setPurchaseOrder(requirePurchaseOrder(req.getPurchaseOrderId()));
        }
        if (req.getGoodsReceiptId() != null) {
            inv.setGoodsReceipt(requireGoodsReceipt(req.getGoodsReceiptId()));
        }
        inv = supplierInvoiceRepository.save(inv);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SupplierInvoiceRequest.Line line : req.getItems()) {
            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setSupplierInvoice(inv);
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(scale(line.getUnitPrice()));
            item.setLineTotal(scale(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))));
            item.setDescription(line.getDescription());
            if (line.getPurchaseOrderItemId() != null) {
                PurchaseOrderItem poi = requirePoItem(line.getPurchaseOrderItemId());
                item.setPurchaseOrderItem(poi);
                item.setItem(poi.getItem());
            } else if (line.getInventoryItemId() != null) {
                item.setItem(requireInventoryItem(line.getInventoryItemId()));
            }
            supplierInvoiceItemRepository.save(item);
            subtotal = subtotal.add(item.getLineTotal());
        }
        inv.setSubtotal(scale(subtotal));
        inv.setTaxAmount(req.getTaxAmount() != null ? scale(req.getTaxAmount()) : BigDecimal.ZERO);
        inv.setTotalAmount(scale(inv.getSubtotal().add(inv.getTaxAmount())));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv = supplierInvoiceRepository.save(inv);
        auditService.log("PROC_SINV_CREATED", MODULE, "SupplierInvoice", inv.getId(), null, inv.getInvoiceNumber());
        return toSupplierInvoiceResponse(inv);
    }

    @Transactional
    public SupplierInvoiceResponse submitSupplierInvoice(Long id) {
        SupplierInvoice inv = requireSupplierInvoice(id);
        SupplierInvoiceLifecycle.assertTransition(inv.getStatus(), SupplierInvoiceStatus.SUBMITTED);
        inv.setStatus(SupplierInvoiceStatus.SUBMITTED);
        inv = supplierInvoiceRepository.save(inv);
        return toSupplierInvoiceResponse(inv);
    }

    @Transactional
    public SupplierInvoiceResponse matchSupplierInvoice(Long id) {
        SupplierInvoice inv = requireSupplierInvoice(id);
        if (inv.getStatus() != SupplierInvoiceStatus.SUBMITTED
                && inv.getStatus() != SupplierInvoiceStatus.MISMATCHED) {
            throw new BusinessException("Invoice must be SUBMITTED or MISMATCHED to run match");
        }
        List<SupplierInvoiceItem> invoiceLines =
                supplierInvoiceItemRepository.findBySupplierInvoiceIdOrderByIdAsc(inv.getId());
        StringBuilder notes = new StringBuilder();
        boolean matched = true;

        if (inv.getPurchaseOrder() == null) {
            matched = false;
            notes.append("Missing purchase order link. ");
        }
        if (inv.getGoodsReceipt() == null) {
            matched = false;
            notes.append("Missing goods receipt link. ");
        }

        for (SupplierInvoiceItem line : invoiceLines) {
            if (line.getPurchaseOrderItem() == null) {
                matched = false;
                notes.append("Line ").append(line.getId()).append(" missing PO item. ");
                continue;
            }
            PurchaseOrderItem poi = line.getPurchaseOrderItem();
            double poQty = poi.getQuantityOrdered() != null ? poi.getQuantityOrdered() : 0.0;
            double grnQty = poi.getQuantityReceived();
            double invQty = line.getQuantity() != null ? line.getQuantity() : 0.0;
            if (Math.abs(poQty - invQty) > 1e-6 || Math.abs(grnQty - invQty) > 1e-6) {
                matched = false;
                notes.append("Qty mismatch on PO line ").append(poi.getId())
                        .append(" (PO=").append(poQty).append(", GRN=").append(grnQty)
                        .append(", INV=").append(invQty).append("). ");
            }
            BigDecimal poPrice = poi.getUnitPrice() != null ? scale(poi.getUnitPrice()) : BigDecimal.ZERO;
            BigDecimal invPrice = line.getUnitPrice() != null ? scale(line.getUnitPrice()) : BigDecimal.ZERO;
            if (poPrice.compareTo(invPrice) != 0) {
                matched = false;
                notes.append("Price mismatch on PO line ").append(poi.getId())
                        .append(" (PO=").append(poPrice).append(", INV=").append(invPrice).append("). ");
            }
            BigDecimal expectedLine = scale(poPrice.multiply(BigDecimal.valueOf(invQty)));
            BigDecimal actualLine = line.getLineTotal() != null ? scale(line.getLineTotal()) : BigDecimal.ZERO;
            if (expectedLine.compareTo(actualLine) != 0) {
                matched = false;
                notes.append("Line total mismatch on PO line ").append(poi.getId()).append(". ");
            }
        }

        if (inv.getPurchaseOrder() != null) {
            BigDecimal poTotal = inv.getPurchaseOrder().getTotalAmount() != null
                    ? scale(inv.getPurchaseOrder().getTotalAmount()) : BigDecimal.ZERO;
            BigDecimal invTotal = inv.getTotalAmount() != null ? scale(inv.getTotalAmount()) : BigDecimal.ZERO;
            // Compare invoice subtotal+tax to PO total when tax is on invoice; strict equality on totals
            if (poTotal.compareTo(invTotal) != 0) {
                // Allow match when line-level already matched and PO has no tax while invoice has tax —
                // still require equality per V1 strict rule
                matched = false;
                notes.append("Header total mismatch (PO=").append(poTotal)
                        .append(", INV=").append(invTotal).append("). ");
            }
        }

        SupplierInvoiceStatus next = matched ? SupplierInvoiceStatus.MATCHED : SupplierInvoiceStatus.MISMATCHED;
        SupplierInvoiceLifecycle.assertTransition(inv.getStatus(), next);
        inv.setStatus(next);
        inv.setMatchStatus(matched ? MatchStatus.MATCHED : MatchStatus.MISMATCHED);
        inv.setMatchNotes(notes.length() == 0 ? (matched ? "3-way match OK" : "Mismatch") : notes.toString().trim());
        inv = supplierInvoiceRepository.save(inv);
        auditService.log("PROC_SINV_MATCHED", MODULE, "SupplierInvoice", inv.getId(),
                null, inv.getMatchStatus().name());
        return toSupplierInvoiceResponse(inv);
    }

    @Transactional
    public SupplierInvoiceResponse approveSupplierInvoice(Long id) {
        SupplierInvoice inv = requireSupplierInvoice(id);
        if (inv.getStatus() != SupplierInvoiceStatus.MATCHED) {
            throw new BusinessException("Only MATCHED invoices can be approved (no finance inventory posting)");
        }
        SupplierInvoiceLifecycle.assertTransition(inv.getStatus(), SupplierInvoiceStatus.APPROVED);
        User me = requireCurrentUser();
        if (inv.getCreatedBy() != null && inv.getCreatedBy().getId().equals(me.getId())) {
            throw new BusinessException("Cannot self-approve a supplier invoice");
        }
        inv.setStatus(SupplierInvoiceStatus.APPROVED);
        inv.setApprovedBy(me);
        inv.setApprovedAt(LocalDateTime.now());
        // Intentionally NO finance post — inventory/AP already recognized on GRN stockIn
        inv = supplierInvoiceRepository.save(inv);
        auditService.log("PROC_SINV_APPROVED", MODULE, "SupplierInvoice", inv.getId(), null, me.getEmail());
        return toSupplierInvoiceResponse(inv);
    }

    @Transactional
    public SupplierInvoiceResponse cancelSupplierInvoice(Long id) {
        SupplierInvoice inv = requireSupplierInvoice(id);
        SupplierInvoiceLifecycle.assertTransition(inv.getStatus(), SupplierInvoiceStatus.CANCELLED);
        inv.setStatus(SupplierInvoiceStatus.CANCELLED);
        inv = supplierInvoiceRepository.save(inv);
        auditService.log("PROC_SINV_CANCELLED", MODULE, "SupplierInvoice", inv.getId(), null, null);
        return toSupplierInvoiceResponse(inv);
    }

    // ─── Payments ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> listPayments() {
        return supplierPaymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierPaymentResponse getPayment(Long id) {
        return toPaymentResponse(requirePayment(id));
    }

    @Transactional
    public SupplierPaymentResponse createPayment(SupplierPaymentRequest req) {
        SupplierInvoice inv = requireSupplierInvoice(req.getSupplierInvoiceId());
        if (inv.getStatus() != SupplierInvoiceStatus.APPROVED
                && inv.getStatus() != SupplierInvoiceStatus.PARTIALLY_PAID) {
            throw new BusinessException("Payments only allowed on APPROVED or PARTIALLY_PAID invoices");
        }
        BigDecimal amount = scale(req.getAmount());
        BigDecimal outstanding = scale(inv.getOutstanding());
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException("Payment exceeds outstanding amount " + outstanding);
        }
        PaymentMethod method = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.BANK_TRANSFER;
        if (method == PaymentMethod.CREDIT) {
            throw new BusinessException("Supplier payment cannot use CREDIT method");
        }
        LocalDate paymentDate = req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now();

        SupplierPayment payment = new SupplierPayment();
        payment.setPaymentNumber(numberService.nextPaymentNumber());
        payment.setSupplierInvoice(inv);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentDate(paymentDate);
        payment.setNotes(req.getNotes());
        payment.setRecordedBy(requireCurrentUser());
        payment = supplierPaymentRepository.saveAndFlush(payment);

        String creditAccount = method == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();
        financeService.postTransaction(TransactionType.TRANSFER, amount, BusinessUnit.GENERAL,
                AccountCode.LIABILITY_AP.getCode(), creditAccount,
                "Supplier payment " + payment.getPaymentNumber() + " for " + inv.getInvoiceNumber(),
                FinanceSourceTypes.MODULE_PROCUREMENT,
                FinanceSourceTypes.REF_PROCUREMENT_PAYMENT,
                payment.getId(),
                method, paymentDate);

        inv.setPaidAmount(scale(nvl(inv.getPaidAmount()).add(amount)));
        BigDecimal remaining = scale(inv.getOutstanding());
        SupplierInvoiceStatus next = remaining.compareTo(BigDecimal.ZERO) <= 0
                ? SupplierInvoiceStatus.PAID
                : SupplierInvoiceStatus.PARTIALLY_PAID;
        SupplierInvoiceLifecycle.assertTransition(inv.getStatus(), next);
        inv.setStatus(next);
        supplierInvoiceRepository.save(inv);

        auditService.log("PROC_PAYMENT_RECORDED", MODULE, "SupplierPayment", payment.getId(),
                null, payment.getPaymentNumber());
        return toPaymentResponse(payment);
    }

    // ─── Dashboard / AP / reports ────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("openRequisitions", requisitionRepository.countByStatus(ProcRequisitionStatus.SUBMITTED)
                + requisitionRepository.countByStatus(ProcRequisitionStatus.UNDER_REVIEW));
        dash.put("draftRequisitions", requisitionRepository.countByStatus(ProcRequisitionStatus.DRAFT));
        long openPos = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.SENT
                        || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED
                        || po.getStatus() == PurchaseOrderStatus.APPROVED
                        || po.getStatus() == PurchaseOrderStatus.SUBMITTED)
                .count();
        dash.put("openPurchaseOrders", openPos);
        dash.put("draftGrns", goodsReceiptRepository.countByStatus(GoodsReceiptStatus.DRAFT));
        dash.put("invoicesAwaitingMatch", supplierInvoiceRepository.countByStatus(SupplierInvoiceStatus.SUBMITTED)
                + supplierInvoiceRepository.countByStatus(SupplierInvoiceStatus.MISMATCHED));
        dash.put("approvedUnpaidInvoices", supplierInvoiceRepository.countByStatus(SupplierInvoiceStatus.APPROVED)
                + supplierInvoiceRepository.countByStatus(SupplierInvoiceStatus.PARTIALLY_PAID));
        BigDecimal apOutstanding = supplierInvoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == SupplierInvoiceStatus.APPROVED
                        || i.getStatus() == SupplierInvoiceStatus.PARTIALLY_PAID)
                .map(SupplierInvoice::getOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dash.put("apOutstanding", scale(apOutstanding));
        return dash;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> apAging() {
        LocalDate today = LocalDate.now();
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal d1_30 = BigDecimal.ZERO;
        BigDecimal d31_60 = BigDecimal.ZERO;
        BigDecimal d61_90 = BigDecimal.ZERO;
        BigDecimal d90Plus = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();

        for (SupplierInvoice inv : supplierInvoiceRepository.findAll()) {
            if (inv.getStatus() != SupplierInvoiceStatus.APPROVED
                    && inv.getStatus() != SupplierInvoiceStatus.PARTIALLY_PAID) {
                continue;
            }
            BigDecimal outstanding = scale(inv.getOutstanding());
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDate due = inv.getDueDate() != null ? inv.getDueDate()
                    : (inv.getInvoiceDate() != null ? inv.getInvoiceDate() : today);
            long days = java.time.temporal.ChronoUnit.DAYS.between(due, today);
            String bucket;
            if (days <= 0) {
                current = current.add(outstanding);
                bucket = "CURRENT";
            } else if (days <= 30) {
                d1_30 = d1_30.add(outstanding);
                bucket = "1_30";
            } else if (days <= 60) {
                d31_60 = d31_60.add(outstanding);
                bucket = "31_60";
            } else if (days <= 90) {
                d61_90 = d61_90.add(outstanding);
                bucket = "61_90";
            } else {
                d90Plus = d90Plus.add(outstanding);
                bucket = "90_PLUS";
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("invoiceId", inv.getId());
            row.put("invoiceNumber", inv.getInvoiceNumber());
            row.put("supplierName", inv.getSupplier() != null ? inv.getSupplier().getName() : null);
            row.put("dueDate", due);
            row.put("daysPastDue", Math.max(0, days));
            row.put("outstanding", outstanding);
            row.put("bucket", bucket);
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", scale(current));
        result.put("days1to30", scale(d1_30));
        result.put("days31to60", scale(d31_60));
        result.put("days61to90", scale(d61_90));
        result.put("days90Plus", scale(d90Plus));
        result.put("total", scale(current.add(d1_30).add(d31_60).add(d61_90).add(d90Plus)));
        result.put("invoices", rows);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> supplierStatement(Long supplierId) {
        Supplier supplier = requireSupplier(supplierId);
        List<SupplierInvoice> invoices = supplierInvoiceRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        List<Map<String, Object>> invoiceRows = new ArrayList<>();
        List<Map<String, Object>> paymentRows = new ArrayList<>();
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (SupplierInvoice inv : invoices) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("invoiceId", inv.getId());
            row.put("invoiceNumber", inv.getInvoiceNumber());
            row.put("supplierInvoiceNumber", inv.getSupplierInvoiceNumber());
            row.put("status", inv.getStatus());
            row.put("invoiceDate", inv.getInvoiceDate());
            row.put("totalAmount", inv.getTotalAmount());
            row.put("paidAmount", inv.getPaidAmount());
            row.put("outstanding", inv.getOutstanding());
            invoiceRows.add(row);
            if (inv.getStatus() != SupplierInvoiceStatus.CANCELLED) {
                billed = billed.add(nvl(inv.getTotalAmount()));
                paid = paid.add(nvl(inv.getPaidAmount()));
            }
            for (SupplierPayment p : supplierPaymentRepository.findBySupplierInvoiceIdOrderByCreatedAtDesc(inv.getId())) {
                Map<String, Object> pr = new LinkedHashMap<>();
                pr.put("paymentId", p.getId());
                pr.put("paymentNumber", p.getPaymentNumber());
                pr.put("invoiceNumber", inv.getInvoiceNumber());
                pr.put("amount", p.getAmount());
                pr.put("paymentDate", p.getPaymentDate());
                pr.put("paymentMethod", p.getPaymentMethod());
                paymentRows.add(pr);
            }
        }
        Map<String, Object> statement = new LinkedHashMap<>();
        statement.put("supplierId", supplier.getId());
        statement.put("supplierCode", supplier.getSupplierCode());
        statement.put("supplierName", supplier.getName());
        statement.put("billed", scale(billed));
        statement.put("paid", scale(paid));
        statement.put("outstanding", scale(billed.subtract(paid)));
        statement.put("invoices", invoiceRows);
        statement.put("payments", paymentRows);
        return statement;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        Map<String, Object> reports = new LinkedHashMap<>();
        reports.put("dashboard", dashboard());
        reports.put("apAging", apAging());
        reports.put("invoiceStatusCounts", Arrays.stream(SupplierInvoiceStatus.values())
                .collect(Collectors.toMap(Enum::name, supplierInvoiceRepository::countByStatus,
                        (a, b) -> a, LinkedHashMap::new)));
        return reports;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    public static String normalizePaymentTerms(String terms) {
        if (terms == null || terms.isBlank()) {
            return "NET_30";
        }
        String t = terms.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!PAYMENT_TERMS.contains(t)) {
            throw new BusinessException("paymentTerms must be one of " + PAYMENT_TERMS);
        }
        return t;
    }

    public static void validatePaymentTerms(String terms) {
        if (terms != null && !terms.isBlank()) {
            normalizePaymentTerms(terms);
        }
    }

    private void applyRequisitionFields(ProcRequisition r, RequisitionRequest req) {
        r.setTitle(req.getTitle());
        r.setJustification(req.getJustification());
        r.setNeededBy(req.getNeededBy());
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            r.setPriority(ProcRequisitionPriority.valueOf(req.getPriority().trim().toUpperCase(Locale.ROOT)));
        } else if (r.getPriority() == null) {
            r.setPriority(ProcRequisitionPriority.NORMAL);
        }
    }

    private void saveRequisitionItems(ProcRequisition r, List<RequisitionRequest.RequisitionItemRequest> items) {
        for (RequisitionRequest.RequisitionItemRequest line : items) {
            InventoryItem item = requireInventoryItem(line.getInventoryItemId());
            ProcRequisitionItem ri = new ProcRequisitionItem();
            ri.setRequisition(r);
            ri.setItem(item);
            ri.setQuantityRequested(line.getQuantityRequested());
            ri.setEstimatedUnitPrice(line.getEstimatedUnitPrice() != null ? scale(line.getEstimatedUnitPrice()) : null);
            ri.setNotes(line.getNotes());
            requisitionItemRepository.save(ri);
        }
    }

    private void applyPoFields(PurchaseOrder po, PurchaseOrderRequest req, Supplier supplier) {
        po.setSupplier(supplier);
        po.setOrderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now());
        po.setExpectedDelivery(req.getExpectedDelivery());
        po.setPaymentTerms(req.getPaymentTerms() != null
                ? normalizePaymentTerms(req.getPaymentTerms())
                : normalizePaymentTerms(supplier.getPaymentTerms()));
        po.setCurrency(req.getCurrency() != null ? req.getCurrency()
                : (supplier.getCurrency() != null ? supplier.getCurrency() : "ETB"));
        po.setNotes(req.getNotes());
        if (req.getRequisitionId() != null) {
            po.setRequisition(requireRequisition(req.getRequisitionId()));
        }
    }

    private void savePoItems(PurchaseOrder po, List<PurchaseOrderRequest.Line> items) {
        for (PurchaseOrderRequest.Line line : items) {
            InventoryItem item = requireInventoryItem(line.getInventoryItemId());
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setPurchaseOrder(po);
            poi.setItem(item);
            poi.setQuantityOrdered(line.getQuantityOrdered());
            poi.setQuantityReceived(0.0);
            poi.setUnitPrice(scale(line.getUnitPrice()));
            poi.setLineTotal(scale(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityOrdered()))));
            poi.setNotes(line.getNotes());
            purchaseOrderItemRepository.save(poi);
        }
    }

    private void recalculatePoTotals(PurchaseOrder po, BigDecimal taxAmount) {
        BigDecimal subtotal = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId()).stream()
                .map(i -> i.getLineTotal() != null ? i.getLineTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setSubtotal(scale(subtotal));
        po.setTaxAmount(taxAmount != null ? scale(taxAmount) : BigDecimal.ZERO);
        po.setTotalAmount(scale(po.getSubtotal().add(po.getTaxAmount())));
    }

    private void ensurePoHasLines(PurchaseOrder po) {
        if (purchaseOrderItemRepository.findByPurchaseOrderId(po.getId()).isEmpty()) {
            throw new BusinessException("Purchase order has no line items");
        }
    }

    private double remainingQty(PurchaseOrderItem poi) {
        double ordered = poi.getQuantityOrdered() != null ? poi.getQuantityOrdered() : 0.0;
        double received = poi.getQuantityReceived();
        return Math.max(0.0, ordered - received);
    }

    private void refreshPoReceiptStatus(PurchaseOrder po) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
        boolean anyReceived = false;
        boolean allReceived = !items.isEmpty();
        for (PurchaseOrderItem item : items) {
            double ordered = item.getQuantityOrdered() != null ? item.getQuantityOrdered() : 0.0;
            double received = item.getQuantityReceived();
            if (received > 1e-9) {
                anyReceived = true;
            }
            if (received + 1e-9 < ordered) {
                allReceived = false;
            }
        }
        PurchaseOrderStatus next;
        if (allReceived) {
            next = PurchaseOrderStatus.FULLY_RECEIVED;
            po.setActualDelivery(LocalDate.now());
        } else if (anyReceived) {
            next = PurchaseOrderStatus.PARTIALLY_RECEIVED;
        } else {
            return;
        }
        if (po.getStatus() != next) {
            PurchaseOrderLifecycle.assertTransition(po.getStatus(), next);
            po.setStatus(next);
            purchaseOrderRepository.save(po);
        }
    }

    private ProcRequisition requireRequisition(Long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found"));
    }

    private PurchaseOrder requirePurchaseOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
    }

    private PurchaseOrderItem requirePoItem(Long id) {
        return purchaseOrderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order item not found"));
    }

    private GoodsReceipt requireGoodsReceipt(Long id) {
        return goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found"));
    }

    private SupplierInvoice requireSupplierInvoice(Long id) {
        return supplierInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier invoice not found"));
    }

    private SupplierPayment requirePayment(Long id) {
        return supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found"));
    }

    private Supplier requireSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    private InventoryItem requireInventoryItem(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private User requireCurrentUser() {
        User u = currentUser();
        if (u == null) {
            throw new BusinessException("Authenticated user required");
        }
        return u;
    }

    private User currentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private RequisitionResponse toRequisitionResponse(ProcRequisition r) {
        List<RequisitionResponse.Item> items = requisitionItemRepository
                .findByRequisitionIdOrderByIdAsc(r.getId()).stream()
                .map(i -> RequisitionResponse.Item.builder()
                        .id(i.getId())
                        .inventoryItemId(i.getItem() != null ? i.getItem().getId() : null)
                        .itemCode(i.getItem() != null ? i.getItem().getSku() : null)
                        .itemName(i.getItem() != null ? i.getItem().getItemName() : null)
                        .quantityRequested(i.getQuantityRequested())
                        .estimatedUnitPrice(i.getEstimatedUnitPrice())
                        .notes(i.getNotes())
                        .build())
                .toList();
        return RequisitionResponse.builder()
                .id(r.getId())
                .requisitionNumber(r.getRequisitionNumber())
                .status(r.getStatus())
                .priority(r.getPriority())
                .title(r.getTitle())
                .justification(r.getJustification())
                .neededBy(r.getNeededBy())
                .requestedById(r.getRequestedBy() != null ? r.getRequestedBy().getId() : null)
                .requestedByName(r.getRequestedBy() != null ? r.getRequestedBy().getFullName() : null)
                .approvedById(r.getApprovedBy() != null ? r.getApprovedBy().getId() : null)
                .approvedByName(r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null)
                .approvedAt(r.getApprovedAt())
                .rejectionReason(r.getRejectionReason())
                .convertedPurchaseOrderId(r.getConvertedPurchaseOrder() != null ? r.getConvertedPurchaseOrder().getId() : null)
                .convertedPoNumber(r.getConvertedPurchaseOrder() != null ? r.getConvertedPurchaseOrder().getPoNumber() : null)
                .createdAt(r.getCreatedAt())
                .items(items)
                .build();
    }

    private PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder po) {
        List<PurchaseOrderResponse.Line> items = purchaseOrderItemRepository
                .findByPurchaseOrderId(po.getId()).stream()
                .map(i -> PurchaseOrderResponse.Line.builder()
                        .id(i.getId())
                        .inventoryItemId(i.getItem() != null ? i.getItem().getId() : null)
                        .itemCode(i.getItem() != null ? i.getItem().getSku() : null)
                        .itemName(i.getItem() != null ? i.getItem().getItemName() : null)
                        .quantityOrdered(i.getQuantityOrdered())
                        .quantityReceived(i.getQuantityReceived())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .notes(i.getNotes())
                        .build())
                .toList();
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierCode(po.getSupplier() != null ? po.getSupplier().getSupplierCode() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .status(po.getStatus())
                .orderDate(po.getOrderDate())
                .paymentTerms(po.getPaymentTerms())
                .currency(po.getCurrency())
                .subtotal(po.getSubtotal())
                .taxAmount(po.getTaxAmount())
                .totalAmount(po.getTotalAmount())
                .expectedDelivery(po.getExpectedDelivery())
                .actualDelivery(po.getActualDelivery())
                .notes(po.getNotes())
                .requisitionId(po.getRequisition() != null ? po.getRequisition().getId() : null)
                .requisitionNumber(po.getRequisition() != null ? po.getRequisition().getRequisitionNumber() : null)
                .createdById(po.getCreatedBy() != null ? po.getCreatedBy().getId() : null)
                .createdByName(po.getCreatedBy() != null ? po.getCreatedBy().getFullName() : null)
                .approvedById(po.getApprovedBy() != null ? po.getApprovedBy().getId() : null)
                .approvedByName(po.getApprovedBy() != null ? po.getApprovedBy().getFullName() : null)
                .approvedAt(po.getApprovedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(items)
                .build();
    }

    private GoodsReceiptResponse toGoodsReceiptResponse(GoodsReceipt grn) {
        List<GoodsReceiptResponse.Line> items = goodsReceiptItemRepository
                .findByGoodsReceiptIdOrderByIdAsc(grn.getId()).stream()
                .map(i -> GoodsReceiptResponse.Line.builder()
                        .id(i.getId())
                        .purchaseOrderItemId(i.getPurchaseOrderItem() != null ? i.getPurchaseOrderItem().getId() : null)
                        .inventoryItemId(i.getItem() != null ? i.getItem().getId() : null)
                        .itemCode(i.getItem() != null ? i.getItem().getSku() : null)
                        .itemName(i.getItem() != null ? i.getItem().getItemName() : null)
                        .receivedQty(i.getReceivedQty())
                        .rejectedQty(i.getRejectedQty())
                        .batchNumber(i.getBatchNumber())
                        .expiryDate(i.getExpiryDate())
                        .manufactureDate(i.getManufactureDate())
                        .unitCost(i.getUnitCost())
                        .stockBatchId(i.getStockBatchId())
                        .build())
                .toList();
        return GoodsReceiptResponse.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getId() : null)
                .poNumber(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoNumber() : null)
                .supplierId(grn.getSupplier() != null ? grn.getSupplier().getId() : null)
                .supplierName(grn.getSupplier() != null ? grn.getSupplier().getName() : null)
                .status(grn.getStatus())
                .receiptDate(grn.getReceiptDate())
                .notes(grn.getNotes())
                .receivedById(grn.getReceivedBy() != null ? grn.getReceivedBy().getId() : null)
                .receivedByName(grn.getReceivedBy() != null ? grn.getReceivedBy().getFullName() : null)
                .postedAt(grn.getPostedAt())
                .createdAt(grn.getCreatedAt())
                .items(items)
                .build();
    }

    private SupplierInvoiceResponse toSupplierInvoiceResponse(SupplierInvoice inv) {
        List<SupplierInvoiceResponse.Line> items = supplierInvoiceItemRepository
                .findBySupplierInvoiceIdOrderByIdAsc(inv.getId()).stream()
                .map(i -> SupplierInvoiceResponse.Line.builder()
                        .id(i.getId())
                        .purchaseOrderItemId(i.getPurchaseOrderItem() != null ? i.getPurchaseOrderItem().getId() : null)
                        .inventoryItemId(i.getItem() != null ? i.getItem().getId() : null)
                        .itemName(i.getItem() != null ? i.getItem().getItemName() : null)
                        .description(i.getDescription())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();
        LocalDate today = LocalDate.now();
        boolean overdue = inv.getDueDate() != null
                && inv.getDueDate().isBefore(today)
                && (inv.getStatus() == SupplierInvoiceStatus.APPROVED
                    || inv.getStatus() == SupplierInvoiceStatus.PARTIALLY_PAID
                    || inv.getStatus() == SupplierInvoiceStatus.MATCHED);
        return SupplierInvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .supplierInvoiceNumber(inv.getSupplierInvoiceNumber())
                .supplierId(inv.getSupplier() != null ? inv.getSupplier().getId() : null)
                .supplierName(inv.getSupplier() != null ? inv.getSupplier().getName() : null)
                .purchaseOrderId(inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getId() : null)
                .poNumber(inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getPoNumber() : null)
                .goodsReceiptId(inv.getGoodsReceipt() != null ? inv.getGoodsReceipt().getId() : null)
                .grnNumber(inv.getGoodsReceipt() != null ? inv.getGoodsReceipt().getGrnNumber() : null)
                .status(inv.getStatus())
                .matchStatus(inv.getMatchStatus())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .overdue(overdue)
                .currency(inv.getCurrency())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .paidAmount(inv.getPaidAmount())
                .outstanding(scale(inv.getOutstanding()))
                .matchNotes(inv.getMatchNotes())
                .notes(inv.getNotes())
                .approvedById(inv.getApprovedBy() != null ? inv.getApprovedBy().getId() : null)
                .approvedByName(inv.getApprovedBy() != null ? inv.getApprovedBy().getFullName() : null)
                .approvedAt(inv.getApprovedAt())
                .createdAt(inv.getCreatedAt())
                .items(items)
                .build();
    }

    private SupplierPaymentResponse toPaymentResponse(SupplierPayment p) {
        SupplierInvoice inv = p.getSupplierInvoice();
        return SupplierPaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .supplierInvoiceId(inv != null ? inv.getId() : null)
                .invoiceNumber(inv != null ? inv.getInvoiceNumber() : null)
                .supplierId(inv != null && inv.getSupplier() != null ? inv.getSupplier().getId() : null)
                .supplierName(inv != null && inv.getSupplier() != null ? inv.getSupplier().getName() : null)
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .paymentDate(p.getPaymentDate())
                .notes(p.getNotes())
                .recordedById(p.getRecordedBy() != null ? p.getRecordedBy().getId() : null)
                .recordedByName(p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
