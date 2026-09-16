package com.trustagro.procurement.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.inventory.entity.PurchaseOrderItem;
import com.trustagro.inventory.entity.PurchaseOrderStatus;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.inventory.service.SupplierService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.dto.GoodsReceiptRequest;
import com.trustagro.procurement.dto.PurchaseOrderRequest;
import com.trustagro.procurement.dto.RequisitionRequest;
import com.trustagro.procurement.dto.SupplierInvoiceRequest;
import com.trustagro.procurement.dto.SupplierPaymentRequest;
import com.trustagro.procurement.entity.*;
import com.trustagro.procurement.repository.*;
import com.trustagro.security.Permissions;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Backend-authority tests for {@link ProcurementService}: requisition → PO → GRN →
 * 3-way match → payment, with inventory stock-in mocked and finance postings asserted
 * by account code.
 */
@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    private static final String AP = AccountCode.LIABILITY_AP.getCode();
    private static final String CASH = AccountCode.ASSET_CASH.getCode();
    private static final String BANK = AccountCode.ASSET_BANK.getCode();

    @Mock ProcNumberService numberService;
    @Mock ProcRequisitionRepository requisitionRepository;
    @Mock ProcRequisitionItemRepository requisitionItemRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock GoodsReceiptRepository goodsReceiptRepository;
    @Mock GoodsReceiptItemRepository goodsReceiptItemRepository;
    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock SupplierInvoiceItemRepository supplierInvoiceItemRepository;
    @Mock SupplierPaymentRepository supplierPaymentRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryService inventoryService;
    @Mock FinanceService financeService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;
    @Mock ProcNumberSequenceRepository sequenceRepository;

    @InjectMocks ProcurementService service;

    private final AtomicLong ids = new AtomicLong(1);

    private User requester;
    private User approver;
    private Supplier supplier;
    private InventoryItem feedItem;

    @BeforeEach
    void setUp() {
        setAuth(Permissions.PROCUREMENT_READ, Permissions.PROCUREMENT_CREATE,
                Permissions.PROCUREMENT_UPDATE, Permissions.PROCUREMENT_APPROVE,
                Permissions.PROCUREMENT_RECEIVE, Permissions.PROCUREMENT_INVOICE,
                Permissions.PROCUREMENT_PAYMENT, Permissions.PROCUREMENT_CANCEL,
                Permissions.PROCUREMENT_REPORT);

        requester = user(1L, "farm@test.com", "Farm Manager");
        approver = user(2L, "inventory@test.com", "Inventory Manager");
        for (User u : List.of(requester, approver)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        setCurrentUser(requester);

        supplier = new Supplier();
        setId(supplier, 10L);
        supplier.setSupplierCode("SUP-000001");
        supplier.setName("Feed Co");
        supplier.setPaymentTerms("NET_30");
        supplier.setCurrency("ETB");
        supplier.setActive(true);
        lenient().when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));

        feedItem = new InventoryItem();
        setId(feedItem, 20L);
        feedItem.setSku("FEED-001");
        feedItem.setItemName("Broiler Feed");
        feedItem.setLastPurchasePrice(new BigDecimal("100.00"));
        lenient().when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(feedItem));

        lenient().when(numberService.nextRequisitionNumber()).thenReturn("REQ-000001", "REQ-000002");
        lenient().when(numberService.nextPoNumber()).thenReturn("PO-000001", "PO-000002");
        lenient().when(numberService.nextGrnNumber()).thenReturn("GRN-000001", "GRN-000002", "GRN-000003");
        lenient().when(numberService.nextSupplierInvoiceNumber()).thenReturn("SINV-000001", "SINV-000002");
        lenient().when(numberService.nextPaymentNumber()).thenReturn("SPAY-000001", "SPAY-000002", "SPAY-000003");
        lenient().when(numberService.nextSupplierCode()).thenReturn("SUP-000001", "SUP-000002", "SUP-000003");

        lenient().when(requisitionRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(requisitionItemRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(purchaseOrderRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(goodsReceiptRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(goodsReceiptItemRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(supplierInvoiceRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(supplierInvoiceItemRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(supplierPaymentRepository.saveAndFlush(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(supplierRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(900L));
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(901L));

        AtomicLong batchIds = new AtomicLong(500);
        lenient().when(inventoryService.stockIn(anyLong(), anyDouble(), any(), any())).thenAnswer(inv -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("batchId", batchIds.getAndIncrement());
            result.put("newStock", inv.getArgument(1));
            return result;
        });
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ─── 1. Requisition create / submit / approve ────────────────

    @Test
    void createSubmitAndApproveRequisition_noSelfApprove() {
        RequisitionRequest req = requisitionRequest(100.0, "100.00");
        when(requisitionItemRepository.findByRequisitionIdOrderByIdAsc(any())).thenReturn(List.of());

        var created = service.createRequisition(req);
        assertEquals(ProcRequisitionStatus.DRAFT, created.getStatus());
        assertEquals("REQ-000001", created.getRequisitionNumber());
        verify(auditService).log(eq("PROC_REQUISITION_CREATED"), eq("PROCUREMENT"),
                eq("ProcRequisition"), any(), isNull(), eq("REQ-000001"));

        ProcRequisition draft = requisitionEntity(created.getId(), ProcRequisitionStatus.DRAFT, requester);
        ProcRequisitionItem line = requisitionLine(draft, 100.0, "100.00");
        when(requisitionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(requisitionItemRepository.findByRequisitionIdOrderByIdAsc(draft.getId())).thenReturn(List.of(line));

        var submitted = service.submitRequisition(draft.getId());
        assertEquals(ProcRequisitionStatus.SUBMITTED, submitted.getStatus());
        verify(notificationService).createRoleNotification(eq("Requisition submitted"),
                contains("REQ-000001"), eq(Role.INVENTORY_MANAGER));
        verify(auditService).log(eq("PROC_REQUISITION_SUBMITTED"), eq("PROCUREMENT"),
                eq("ProcRequisition"), eq(draft.getId()), isNull(), eq("SUBMITTED"));

        // Self-approve blocked
        assertThrows(BusinessException.class, () -> service.approveRequisition(draft.getId()));

        setCurrentUser(approver);
        draft.setStatus(ProcRequisitionStatus.SUBMITTED);
        var approved = service.approveRequisition(draft.getId());
        assertEquals(ProcRequisitionStatus.APPROVED, approved.getStatus());
        verify(auditService).log(eq("PROC_REQUISITION_APPROVED"), eq("PROCUREMENT"),
                eq("ProcRequisition"), eq(draft.getId()), isNull(), eq(approver.getEmail()));
    }

    // ─── 2. Requisition → PO conversion ──────────────────────────

    @Test
    void convertRequisitionToPo_andDuplicateConversionIsIdempotent() {
        ProcRequisition approved = requisitionEntity(1L, ProcRequisitionStatus.APPROVED, requester);
        ProcRequisitionItem line = requisitionLine(approved, 100.0, "100.00");
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(approved));
        when(requisitionItemRepository.findByRequisitionIdOrderByIdAsc(1L)).thenReturn(List.of(line));

        List<PurchaseOrderItem> savedPoItems = new java.util.ArrayList<>();
        when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> {
            PurchaseOrderItem poi = withId(inv.getArgument(0));
            savedPoItems.add(poi);
            return poi;
        });
        when(purchaseOrderItemRepository.findByPurchaseOrderId(any())).thenAnswer(inv -> List.copyOf(savedPoItems));

        setCurrentUser(approver);
        var po = service.convertRequisitionToPo(1L, 10L);
        assertEquals(PurchaseOrderStatus.DRAFT, po.getStatus());
        assertEquals("PO-000001", po.getPoNumber());
        assertEquals(0, new BigDecimal("10000.00").compareTo(po.getTotalAmount()));
        assertEquals(ProcRequisitionStatus.CONVERTED, approved.getStatus());
        assertNotNull(approved.getConvertedPurchaseOrder());
        verify(auditService).log(eq("PROC_REQUISITION_CONVERTED"), eq("PROCUREMENT"),
                eq("ProcRequisition"), eq(1L), eq("REQ-000001"), eq("PO-000001"));

        // Second conversion returns existing PO without creating another
        clearInvocations(purchaseOrderRepository);
        var again = service.convertRequisitionToPo(1L, 10L);
        assertEquals(po.getId(), again.getId());
        assertEquals("PO-000001", again.getPoNumber());
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ─── 3. PO submit / approve / send ───────────────────────────

    @Test
    void purchaseOrderSubmitApproveSendLifecycle() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.DRAFT, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        when(purchaseOrderRepository.findById(po.getId())).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(poi));

        setCurrentUser(approver);
        assertEquals(PurchaseOrderStatus.SUBMITTED, service.submitPurchaseOrder(po.getId()).getStatus());
        assertEquals(PurchaseOrderStatus.APPROVED, service.approvePurchaseOrder(po.getId()).getStatus());
        verify(auditService).log(eq("PROC_PO_APPROVED"), eq("PROCUREMENT"),
                eq("PurchaseOrder"), eq(po.getId()), isNull(), eq(approver.getEmail()));
        verify(notificationService).createRoleNotification(eq("PO approved"),
                contains(po.getPoNumber()), eq(Role.INVENTORY_MANAGER));

        assertEquals(PurchaseOrderStatus.SENT, service.sendPurchaseOrder(po.getId()).getStatus());
        verify(auditService).log(eq("PROC_PO_SENT"), eq("PROCUREMENT"),
                eq("PurchaseOrder"), eq(po.getId()), isNull(), eq(po.getPoNumber()));
    }

    // ─── 4–7. GRN create / post / partial / over-receive / idempotent

    @Test
    void createAndPostGrn_callsStockIn() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.SENT, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        when(purchaseOrderRepository.findById(po.getId())).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findById(poi.getId())).thenReturn(Optional.of(poi));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(poi));

        GoodsReceiptRequest req = grnRequest(po.getId(), poi.getId(), 100.0, "100.00");
        when(goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(any())).thenAnswer(inv -> {
            GoodsReceiptItem gri = new GoodsReceiptItem();
            setId(gri, 70L);
            gri.setPurchaseOrderItem(poi);
            gri.setItem(feedItem);
            gri.setReceivedQty(100.0);
            gri.setUnitCost(new BigDecimal("100.00"));
            gri.setBatchNumber("BATCH-1");
            return List.of(gri);
        });

        setCurrentUser(approver);
        var created = service.createGoodsReceipt(req);
        assertEquals(GoodsReceiptStatus.DRAFT, created.getStatus());
        verify(auditService).log(eq("PROC_GRN_CREATED"), eq("PROCUREMENT"),
                eq("GoodsReceipt"), any(), isNull(), startsWith("GRN-"));

        GoodsReceipt grn = goodsReceipt(created.getId(), po, GoodsReceiptStatus.DRAFT);
        when(goodsReceiptRepository.findById(grn.getId())).thenReturn(Optional.of(grn));

        var posted = service.postGoodsReceipt(grn.getId());
        assertEquals(GoodsReceiptStatus.POSTED, posted.getStatus());
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, po.getStatus());
        verify(inventoryService, times(1)).stockIn(eq(20L), eq(100.0), anyMap(), eq(approver));
        verify(auditService).log(eq("GRN_POSTED"), eq("PROCUREMENT"),
                eq("GoodsReceipt"), eq(grn.getId()), isNull(), eq(grn.getGrnNumber()));
        verify(notificationService).createRoleNotification(eq("GRN posted"),
                contains(grn.getGrnNumber()), eq(Role.FINANCE));
    }

    @Test
    void partialReceivingUpdatesPoToPartiallyThenFullyReceived() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.SENT, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        when(purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(poi));

        setCurrentUser(approver);

        // First GRN: 40
        GoodsReceipt grn1 = goodsReceipt(81L, po, GoodsReceiptStatus.DRAFT);
        when(goodsReceiptRepository.findById(81L)).thenReturn(Optional.of(grn1));
        when(goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(81L)).thenReturn(
                List.of(grnLine(poi, 40.0, "100.00")));

        service.postGoodsReceipt(81L);
        assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, po.getStatus());
        assertEquals(40.0, poi.getQuantityReceived(), 1e-9);
        verify(inventoryService, times(1)).stockIn(eq(20L), eq(40.0), anyMap(), eq(approver));

        // Second GRN: remaining 60
        GoodsReceipt grn2 = goodsReceipt(82L, po, GoodsReceiptStatus.DRAFT);
        when(goodsReceiptRepository.findById(82L)).thenReturn(Optional.of(grn2));
        when(goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(82L)).thenReturn(
                List.of(grnLine(poi, 60.0, "100.00")));

        service.postGoodsReceipt(82L);
        assertEquals(PurchaseOrderStatus.FULLY_RECEIVED, po.getStatus());
        assertEquals(100.0, poi.getQuantityReceived(), 1e-9);
        verify(inventoryService, times(1)).stockIn(eq(20L), eq(60.0), anyMap(), eq(approver));
        verify(inventoryService, times(2)).stockIn(anyLong(), anyDouble(), any(), any());
    }

    @Test
    void overReceivingRejected_noStockIn() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.SENT, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        when(purchaseOrderRepository.findById(po.getId())).thenReturn(Optional.of(po));
        when(purchaseOrderItemRepository.findById(poi.getId())).thenReturn(Optional.of(poi));

        GoodsReceiptRequest req = grnRequest(po.getId(), poi.getId(), 101.0, "100.00");
        setCurrentUser(approver);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createGoodsReceipt(req));
        assertTrue(ex.getMessage().contains("Cannot receive more than remaining"));
        verify(inventoryService, never()).stockIn(anyLong(), anyDouble(), any(), any());
        verify(goodsReceiptItemRepository, never()).save(any());
    }

    @Test
    void duplicateGrnPostIsIdempotent_stockInOnce() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.SENT, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        GoodsReceipt grn = goodsReceipt(90L, po, GoodsReceiptStatus.DRAFT);
        when(goodsReceiptRepository.findById(90L)).thenReturn(Optional.of(grn));
        when(goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(90L)).thenReturn(
                List.of(grnLine(poi, 100.0, "100.00")));
        when(purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())).thenReturn(List.of(poi));

        setCurrentUser(approver);
        service.postGoodsReceipt(90L);
        assertEquals(GoodsReceiptStatus.POSTED, grn.getStatus());

        var second = service.postGoodsReceipt(90L);
        assertEquals(GoodsReceiptStatus.POSTED, second.getStatus());
        verify(inventoryService, times(1)).stockIn(anyLong(), anyDouble(), any(), any());
        verify(auditService, times(1)).log(eq("GRN_POSTED"), any(), any(), any(), any(), any());
    }

    // ─── 8–10. Three-way match & invoice approve ─────────────────

    @Test
    void threeWayMatch_matchedWhenPoGrnInvoiceAlign() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.FULLY_RECEIVED, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        poi.setQuantityReceived(100.0);
        GoodsReceipt grn = goodsReceipt(91L, po, GoodsReceiptStatus.POSTED);

        SupplierInvoice inv = supplierInvoice(100L, po, grn, "10000.00", SupplierInvoiceStatus.SUBMITTED);
        SupplierInvoiceItem line = invoiceLine(inv, poi, 100.0, "100.00");
        when(supplierInvoiceRepository.findById(inv.getId())).thenReturn(Optional.of(inv));
        when(supplierInvoiceItemRepository.findBySupplierInvoiceIdOrderByIdAsc(inv.getId()))
                .thenReturn(List.of(line));

        var result = service.matchSupplierInvoice(inv.getId());
        assertEquals(SupplierInvoiceStatus.MATCHED, result.getStatus());
        assertEquals(MatchStatus.MATCHED, result.getMatchStatus());
        verify(auditService).log(eq("PROC_SINV_MATCHED"), eq("PROCUREMENT"),
                eq("SupplierInvoice"), eq(inv.getId()), isNull(), eq("MATCHED"));
    }

    @Test
    void threeWayMatch_mismatchedWhenGrn90VsInvoice100_cannotApprove() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.PARTIALLY_RECEIVED, "10000.00");
        PurchaseOrderItem poi = poItem(po, 100.0, "100.00");
        poi.setQuantityReceived(90.0); // GRN received 90
        GoodsReceipt grn = goodsReceipt(92L, po, GoodsReceiptStatus.POSTED);

        SupplierInvoice inv = supplierInvoice(101L, po, grn, "10000.00", SupplierInvoiceStatus.SUBMITTED);
        SupplierInvoiceItem line = invoiceLine(inv, poi, 100.0, "100.00");
        when(supplierInvoiceRepository.findById(inv.getId())).thenReturn(Optional.of(inv));
        when(supplierInvoiceItemRepository.findBySupplierInvoiceIdOrderByIdAsc(inv.getId()))
                .thenReturn(List.of(line));

        var result = service.matchSupplierInvoice(inv.getId());
        assertEquals(SupplierInvoiceStatus.MISMATCHED, result.getStatus());
        assertEquals(MatchStatus.MISMATCHED, result.getMatchStatus());
        assertTrue(result.getMatchNotes().contains("Qty mismatch"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approveSupplierInvoice(inv.getId()));
        assertTrue(ex.getMessage().contains("Only MATCHED"));
    }

    @Test
    void approveSupplierInvoice_doesNotCallPostInventoryReceiptOrStockIn() {
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.FULLY_RECEIVED, "10000.00");
        GoodsReceipt grn = goodsReceipt(93L, po, GoodsReceiptStatus.POSTED);
        SupplierInvoice inv = new SupplierInvoice();
        setId(inv, 200L);
        inv.setInvoiceNumber("SINV-000001");
        inv.setSupplier(supplier);
        inv.setPurchaseOrder(po);
        inv.setGoodsReceipt(grn);
        inv.setStatus(SupplierInvoiceStatus.MATCHED);
        inv.setMatchStatus(MatchStatus.MATCHED);
        inv.setTotalAmount(new BigDecimal("10000.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        when(supplierInvoiceRepository.findById(200L)).thenReturn(Optional.of(inv));

        setCurrentUser(approver);
        var approved = service.approveSupplierInvoice(200L);
        assertEquals(SupplierInvoiceStatus.APPROVED, approved.getStatus());

        verify(financeService, never()).postInventoryReceipt(any(), any(), any(), any(), any());
        verify(inventoryService, never()).stockIn(anyLong(), anyDouble(), any(), any());
        verify(auditService).log(eq("PROC_SINV_APPROVED"), eq("PROCUREMENT"),
                eq("SupplierInvoice"), eq(200L), isNull(), eq(approver.getEmail()));
    }

    // ─── 11–14. Payments & accounting ────────────────────────────

    @Test
    void paymentPostsDrApCrBank_refProcurementPayment_noInventoryDebit() {
        SupplierInvoice inv = approvedInvoice(new BigDecimal("100000.00"));
        when(supplierInvoiceRepository.findById(inv.getId())).thenReturn(Optional.of(inv));

        SupplierPaymentRequest req = new SupplierPaymentRequest();
        req.setSupplierInvoiceId(inv.getId());
        req.setAmount(new BigDecimal("100000.00"));
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        setCurrentUser(approver);
        var payment = service.createPayment(req);
        assertNotNull(payment.getPaymentNumber());
        assertEquals(SupplierInvoiceStatus.PAID, inv.getStatus());

        verify(financeService).postTransaction(
                eq(TransactionType.TRANSFER),
                eq(new BigDecimal("100000.00")),
                eq(BusinessUnit.GENERAL),
                eq(AP),
                eq(BANK),
                contains("Supplier payment"),
                eq(FinanceSourceTypes.MODULE_PROCUREMENT),
                eq(FinanceSourceTypes.REF_PROCUREMENT_PAYMENT),
                any(),
                eq(PaymentMethod.BANK_TRANSFER),
                any());
        verify(financeService, never()).postInventoryReceipt(any(), any(), any(), any(), any());
        verify(inventoryService, never()).stockIn(anyLong(), anyDouble(), any(), any());
        verify(auditService).log(eq("PROC_PAYMENT_RECORDED"), eq("PROCUREMENT"),
                eq("SupplierPayment"), any(), isNull(), startsWith("SPAY-"));
    }

    @Test
    void accountingScenario_afterGrnApRecognized_paymentSettlesAp_noSecondInventoryOnInvoiceApprove() {
        // Simulate: GRN stockIn already recognized Dr Inventory / Cr AP 100000 outside this unit.
        // Invoice approve must not re-post inventory; payment settles AP → Bank.
        PurchaseOrder po = purchaseOrder(PurchaseOrderStatus.FULLY_RECEIVED, "100000.00");
        GoodsReceipt grn = goodsReceipt(94L, po, GoodsReceiptStatus.POSTED);

        SupplierInvoice inv = new SupplierInvoice();
        setId(inv, 210L);
        inv.setInvoiceNumber("SINV-000010");
        inv.setSupplierInvoiceNumber("SUPP-INV-10");
        inv.setSupplier(supplier);
        inv.setPurchaseOrder(po);
        inv.setGoodsReceipt(grn);
        inv.setStatus(SupplierInvoiceStatus.MATCHED);
        inv.setMatchStatus(MatchStatus.MATCHED);
        inv.setTotalAmount(new BigDecimal("100000.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        when(supplierInvoiceRepository.findById(210L)).thenReturn(Optional.of(inv));

        setCurrentUser(approver);
        service.approveSupplierInvoice(210L);
        verify(financeService, never()).postInventoryReceipt(any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        SupplierPaymentRequest pay = new SupplierPaymentRequest();
        pay.setSupplierInvoiceId(210L);
        pay.setAmount(new BigDecimal("100000.00"));
        pay.setPaymentMethod(PaymentMethod.CASH);

        service.createPayment(pay);

        ArgumentCaptor<String> debit = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> credit = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> refType = ArgumentCaptor.forClass(String.class);
        verify(financeService).postTransaction(
                eq(TransactionType.TRANSFER),
                eq(new BigDecimal("100000.00")),
                eq(BusinessUnit.GENERAL),
                debit.capture(),
                credit.capture(),
                anyString(),
                eq(FinanceSourceTypes.MODULE_PROCUREMENT),
                refType.capture(),
                any(),
                eq(PaymentMethod.CASH),
                any());
        assertEquals(AP, debit.getValue());
        assertEquals(CASH, credit.getValue());
        assertEquals(FinanceSourceTypes.REF_PROCUREMENT_PAYMENT, refType.getValue());
        assertEquals(SupplierInvoiceStatus.PAID, inv.getStatus());
        verify(financeService, never()).postInventoryReceipt(any(), any(), any(), any(), any());
    }

    @Test
    void partialPayment_40kThen60kOn100kInvoice() {
        SupplierInvoice inv = approvedInvoice(new BigDecimal("100000.00"));
        when(supplierInvoiceRepository.findById(inv.getId())).thenReturn(Optional.of(inv));

        setCurrentUser(approver);

        SupplierPaymentRequest first = new SupplierPaymentRequest();
        first.setSupplierInvoiceId(inv.getId());
        first.setAmount(new BigDecimal("40000.00"));
        first.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.createPayment(first);
        assertEquals(SupplierInvoiceStatus.PARTIALLY_PAID, inv.getStatus());
        assertEquals(0, new BigDecimal("40000.00").compareTo(inv.getPaidAmount()));
        assertEquals(0, new BigDecimal("60000.00").compareTo(inv.getOutstanding()));

        SupplierPaymentRequest second = new SupplierPaymentRequest();
        second.setSupplierInvoiceId(inv.getId());
        second.setAmount(new BigDecimal("60000.00"));
        second.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.createPayment(second);
        assertEquals(SupplierInvoiceStatus.PAID, inv.getStatus());
        assertEquals(0, new BigDecimal("100000.00").compareTo(inv.getPaidAmount()));

        verify(financeService, times(2)).postTransaction(
                eq(TransactionType.TRANSFER), any(), eq(BusinessUnit.GENERAL),
                eq(AP), eq(BANK), anyString(),
                eq(FinanceSourceTypes.MODULE_PROCUREMENT),
                eq(FinanceSourceTypes.REF_PROCUREMENT_PAYMENT),
                any(), any(), any());
    }

    @Test
    void duplicateOrOverPaymentPrevented_oncePaid() {
        SupplierInvoice inv = approvedInvoice(new BigDecimal("100000.00"));
        when(supplierInvoiceRepository.findById(inv.getId())).thenReturn(Optional.of(inv));

        setCurrentUser(approver);
        SupplierPaymentRequest full = new SupplierPaymentRequest();
        full.setSupplierInvoiceId(inv.getId());
        full.setAmount(new BigDecimal("100000.00"));
        full.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.createPayment(full);
        assertEquals(SupplierInvoiceStatus.PAID, inv.getStatus());

        // Second payment against PAID invoice rejected
        SupplierPaymentRequest dup = new SupplierPaymentRequest();
        dup.setSupplierInvoiceId(inv.getId());
        dup.setAmount(new BigDecimal("1.00"));
        dup.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        BusinessException paidEx = assertThrows(BusinessException.class, () -> service.createPayment(dup));
        assertTrue(paidEx.getMessage().contains("Payments only allowed"));

        // Over-payment while APPROVED also rejected
        SupplierInvoice open = approvedInvoice(new BigDecimal("50000.00"));
        setId(open, 220L);
        when(supplierInvoiceRepository.findById(220L)).thenReturn(Optional.of(open));
        SupplierPaymentRequest over = new SupplierPaymentRequest();
        over.setSupplierInvoiceId(220L);
        over.setAmount(new BigDecimal("50000.01"));
        over.setPaymentMethod(PaymentMethod.CASH);
        BusinessException overEx = assertThrows(BusinessException.class, () -> service.createPayment(over));
        assertTrue(overEx.getMessage().contains("Payment exceeds outstanding"));

        verify(financeService, times(1)).postTransaction(
                any(), any(), any(), any(), any(), any(), any(),
                eq(FinanceSourceTypes.REF_PROCUREMENT_PAYMENT), any(), any(), any());
    }

    // ─── 18. Audit + notifications already covered above; extra smoke

    @Test
    void createPurchaseOrder_auditsCreation() {
        PurchaseOrderRequest req = new PurchaseOrderRequest();
        req.setSupplierId(10L);
        req.setPaymentTerms("NET_30");
        PurchaseOrderRequest.Line line = new PurchaseOrderRequest.Line();
        line.setInventoryItemId(20L);
        line.setQuantityOrdered(50.0);
        line.setUnitPrice(new BigDecimal("100.00"));
        req.setItems(List.of(line));

        when(purchaseOrderItemRepository.findByPurchaseOrderId(any())).thenAnswer(inv -> {
            PurchaseOrderItem poi = new PurchaseOrderItem();
            setId(poi, 55L);
            poi.setItem(feedItem);
            poi.setQuantityOrdered(50.0);
            poi.setQuantityReceived(0.0);
            poi.setUnitPrice(new BigDecimal("100.00"));
            poi.setLineTotal(new BigDecimal("5000.00"));
            return List.of(poi);
        });

        setCurrentUser(approver);
        var created = service.createPurchaseOrder(req);
        assertEquals(PurchaseOrderStatus.DRAFT, created.getStatus());
        verify(auditService).log(eq("PROC_PO_CREATED"), eq("PROCUREMENT"),
                eq("PurchaseOrder"), any(), isNull(), startsWith("PO-"));
    }

    // ─── 19. Supplier code uniqueness ────────────────────────────

    @Test
    void supplierCodeGenerationIsUniqueAcrossCreates() {
        Map<String, ProcNumberSequence> sequences = new HashMap<>();
        when(sequenceRepository.findByNameForUpdate(anyString())).thenAnswer(inv ->
                Optional.of(sequences.computeIfAbsent(inv.getArgument(0), name -> {
                    ProcNumberSequence seq = new ProcNumberSequence();
                    seq.setName(name);
                    seq.setNextValue(1L);
                    return seq;
                })));
        when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcNumberService realNumbers = new ProcNumberService(sequenceRepository);
        SupplierService supplierService = new SupplierService(supplierRepository, realNumbers, auditService);

        Supplier a = new Supplier();
        a.setName("Alpha Feeds");
        a.setPaymentTerms("NET_30");
        Supplier savedA = supplierService.create(a);
        assertEquals("SUP-000001", savedA.getSupplierCode());

        Supplier b = new Supplier();
        b.setName("Beta Feeds");
        b.setPaymentTerms("NET_15");
        Supplier savedB = supplierService.create(b);
        assertEquals("SUP-000002", savedB.getSupplierCode());
        assertNotEquals(savedA.getSupplierCode(), savedB.getSupplierCode());
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private RequisitionRequest requisitionRequest(double qty, String unitPrice) {
        RequisitionRequest req = new RequisitionRequest();
        req.setTitle("Feed order");
        req.setJustification("Restock broiler feed");
        req.setPriority("HIGH");
        RequisitionRequest.RequisitionItemRequest item = new RequisitionRequest.RequisitionItemRequest();
        item.setInventoryItemId(20L);
        item.setQuantityRequested(qty);
        item.setEstimatedUnitPrice(new BigDecimal(unitPrice));
        req.setItems(List.of(item));
        return req;
    }

    private ProcRequisition requisitionEntity(Long id, ProcRequisitionStatus status, User requestedBy) {
        ProcRequisition r = new ProcRequisition();
        setId(r, id);
        r.setRequisitionNumber("REQ-000001");
        r.setStatus(status);
        r.setPriority(ProcRequisitionPriority.HIGH);
        r.setTitle("Feed order");
        r.setRequestedBy(requestedBy);
        return r;
    }

    private ProcRequisitionItem requisitionLine(ProcRequisition r, double qty, String price) {
        ProcRequisitionItem line = new ProcRequisitionItem();
        setId(line, 30L);
        line.setRequisition(r);
        line.setItem(feedItem);
        line.setQuantityRequested(qty);
        line.setEstimatedUnitPrice(new BigDecimal(price));
        return line;
    }

    private PurchaseOrder purchaseOrder(PurchaseOrderStatus status, String total) {
        PurchaseOrder po = new PurchaseOrder();
        setId(po, 40L);
        po.setPoNumber("PO-000001");
        po.setSupplier(supplier);
        po.setStatus(status);
        po.setOrderDate(LocalDate.now());
        po.setPaymentTerms("NET_30");
        po.setCurrency("ETB");
        po.setSubtotal(new BigDecimal(total));
        po.setTaxAmount(BigDecimal.ZERO.setScale(2));
        po.setTotalAmount(new BigDecimal(total));
        po.setCreatedBy(requester);
        return po;
    }

    private PurchaseOrderItem poItem(PurchaseOrder po, double ordered, String unitPrice) {
        PurchaseOrderItem poi = new PurchaseOrderItem();
        setId(poi, 50L);
        poi.setPurchaseOrder(po);
        poi.setItem(feedItem);
        poi.setQuantityOrdered(ordered);
        poi.setQuantityReceived(0.0);
        poi.setUnitPrice(new BigDecimal(unitPrice));
        poi.setLineTotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(ordered)).setScale(2));
        return poi;
    }

    private GoodsReceiptRequest grnRequest(Long poId, Long poiId, double qty, String unitCost) {
        GoodsReceiptRequest req = new GoodsReceiptRequest();
        req.setPurchaseOrderId(poId);
        GoodsReceiptRequest.Line line = new GoodsReceiptRequest.Line();
        line.setPurchaseOrderItemId(poiId);
        line.setReceivedQty(qty);
        line.setUnitCost(new BigDecimal(unitCost));
        line.setBatchNumber("BATCH-A");
        req.setItems(List.of(line));
        return req;
    }

    private GoodsReceipt goodsReceipt(Long id, PurchaseOrder po, GoodsReceiptStatus status) {
        GoodsReceipt grn = new GoodsReceipt();
        setId(grn, id);
        grn.setGrnNumber("GRN-" + String.format("%06d", id));
        grn.setPurchaseOrder(po);
        grn.setSupplier(supplier);
        grn.setStatus(status);
        grn.setReceiptDate(LocalDate.now());
        grn.setReceivedBy(approver);
        return grn;
    }

    private GoodsReceiptItem grnLine(PurchaseOrderItem poi, double qty, String unitCost) {
        GoodsReceiptItem gri = new GoodsReceiptItem();
        setId(gri, ids.getAndIncrement());
        gri.setPurchaseOrderItem(poi);
        gri.setItem(feedItem);
        gri.setReceivedQty(qty);
        gri.setUnitCost(new BigDecimal(unitCost));
        gri.setBatchNumber("BATCH-" + gri.getId());
        return gri;
    }

    private SupplierInvoice supplierInvoice(Long id, PurchaseOrder po, GoodsReceipt grn,
                                            String total, SupplierInvoiceStatus status) {
        SupplierInvoice inv = new SupplierInvoice();
        setId(inv, id);
        inv.setInvoiceNumber("SINV-" + String.format("%06d", id));
        inv.setSupplierInvoiceNumber("VENDOR-" + id);
        inv.setSupplier(supplier);
        inv.setPurchaseOrder(po);
        inv.setGoodsReceipt(grn);
        inv.setStatus(status);
        inv.setMatchStatus(MatchStatus.PENDING);
        inv.setSubtotal(new BigDecimal(total));
        inv.setTaxAmount(BigDecimal.ZERO.setScale(2));
        inv.setTotalAmount(new BigDecimal(total));
        inv.setPaidAmount(BigDecimal.ZERO.setScale(2));
        return inv;
    }

    private SupplierInvoiceItem invoiceLine(SupplierInvoice inv, PurchaseOrderItem poi,
                                            double qty, String unitPrice) {
        SupplierInvoiceItem line = new SupplierInvoiceItem();
        setId(line, ids.getAndIncrement());
        line.setSupplierInvoice(inv);
        line.setPurchaseOrderItem(poi);
        line.setItem(feedItem);
        line.setQuantity(qty);
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setLineTotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(qty)).setScale(2));
        return line;
    }

    private SupplierInvoice approvedInvoice(BigDecimal total) {
        SupplierInvoice inv = new SupplierInvoice();
        setId(inv, 300L);
        inv.setInvoiceNumber("SINV-000100");
        inv.setSupplierInvoiceNumber("V-100");
        inv.setSupplier(supplier);
        inv.setStatus(SupplierInvoiceStatus.APPROVED);
        inv.setMatchStatus(MatchStatus.MATCHED);
        inv.setTotalAmount(total);
        inv.setPaidAmount(BigDecimal.ZERO.setScale(2));
        inv.setSubtotal(total);
        inv.setTaxAmount(BigDecimal.ZERO.setScale(2));
        return inv;
    }

    private User user(Long id, String email, String name) {
        User u = new User();
        setId(u, id);
        u.setEmail(email);
        u.setFullName(name);
        return u;
    }

    private void setCurrentUser(User u) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                u.getEmail(), "x",
                List.of(new SimpleGrantedAuthority(Permissions.PROCUREMENT_READ)));
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setAuth(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "farm@test.com", "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private FinanceTransaction tx(Long id) {
        FinanceTransaction t = new FinanceTransaction();
        setId(t, id);
        return t;
    }

    private <T> T withId(T entity) {
        if (entity == null) {
            return null;
        }
        try {
            var field = idField(entity.getClass());
            if (field.get(entity) == null) {
                field.set(entity, ids.getAndIncrement());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entity;
    }

    private static void setId(Object entity, Long id) {
        try {
            idField(entity.getClass()).set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static java.lang.reflect.Field idField(Class<?> type) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try {
                var field = c.getDeclaredField("id");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }
}
