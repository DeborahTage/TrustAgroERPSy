package com.trustagro.qa.e2e;

import com.trustagro.finance.entity.*;
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
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.dto.SupplierPaymentRequest;
import com.trustagro.procurement.entity.*;
import com.trustagro.procurement.repository.*;
import com.trustagro.procurement.service.ProcNumberService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.qa.fixture.QaEntities;
import com.trustagro.qa.fixture.QaFinanceHarness;
import com.trustagro.security.Permissions;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Golden procurement accounting: GRN recognizes Inventory/AP; payment settles AP;
 * supplier invoice approve must not re-post inventory or revenue.
 */
@ExtendWith(MockitoExtension.class)
class GoldenProcurementApFinanceScenarioTest {

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
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    private QaFinanceHarness finance;
    private ProcurementService service;
    private final AtomicLong ids = new AtomicLong(1);
    private final AtomicInteger stockInCalls = new AtomicInteger();
    private User approver;
    private Supplier supplier;
    private InventoryItem feedItem;

    @BeforeEach
    void setUp() {
        finance = new QaFinanceHarness();
        setAuth();

        User requester = QaEntities.user(1L, "farm@test.com", "Farm", Role.FARM_MANAGER);
        approver = QaEntities.user(2L, "inventory@test.com", "Inventory", Role.INVENTORY_MANAGER);
        for (User u : List.of(requester, approver)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        setCurrentUser(approver);

        supplier = QaEntities.supplier(10L, "SUP-1", "Feed Co");
        feedItem = QaEntities.item(20L, "FEED-1", "Layer Feed", 0.0, new BigDecimal("100.00"));

        when(inventoryService.stockIn(anyLong(), anyDouble(), anyMap(), any())).thenAnswer(inv -> {
            stockInCalls.incrementAndGet();
            double qty = inv.getArgument(1);
            Map<String, Object> data = inv.getArgument(2);
            BigDecimal unitCost = data.get("unitCost") != null
                    ? new BigDecimal(data.get("unitCost").toString())
                    : new BigDecimal("100.00");
            BigDecimal amount = unitCost.multiply(BigDecimal.valueOf(qty));
            finance.finance().postInventoryReceipt(
                    amount,
                    "GRN stock-in",
                    FinanceSourceTypes.MODULE_PROCUREMENT,
                    FinanceSourceTypes.REF_PROCUREMENT_GRN,
                    ((Number) data.get("referenceId")).longValue());
            feedItem.setCurrentStock(feedItem.getCurrentStock() + qty);
            Map<String, Object> result = new HashMap<>();
            result.put("batchId", 500L);
            return result;
        });

        lenient().when(numberService.nextPaymentNumber()).thenReturn("SPAY-000001", "SPAY-000002");
        lenient().when(numberService.nextGrnNumber()).thenReturn("GRN-000001");
        lenient().when(numberService.nextSupplierInvoiceNumber()).thenReturn("SINV-000001");
        lenient().when(supplierPaymentRepository.save(any())).thenAnswer(inv -> {
            SupplierPayment p = inv.getArgument(0);
            if (p.getId() == null) QaEntities.setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(supplierPaymentRepository.saveAndFlush(any())).thenAnswer(inv -> {
            SupplierPayment p = inv.getArgument(0);
            if (p.getId() == null) QaEntities.setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(purchaseOrderItemRepository.findByPurchaseOrderId(anyLong())).thenAnswer(inv ->
                List.of());
        lenient().when(supplierInvoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(goodsReceiptItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(purchaseOrderItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(purchaseOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new ProcurementService(
                numberService, requisitionRepository, requisitionItemRepository,
                purchaseOrderRepository, purchaseOrderItemRepository,
                goodsReceiptRepository, goodsReceiptItemRepository,
                supplierInvoiceRepository, supplierInvoiceItemRepository,
                supplierPaymentRepository, supplierRepository, inventoryItemRepository,
                inventoryService, finance.finance(), finance.auditService(),
                notificationService, userRepository);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void golden_grnRecognizesAp_invoiceApproveNoRepost_paymentSettlesAp() {
        PurchaseOrder po = new PurchaseOrder();
        QaEntities.setId(po, 30L);
        po.setPoNumber("PO-1");
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        po.setTotalAmount(new BigDecimal("10000.00"));

        PurchaseOrderItem poi = new PurchaseOrderItem();
        QaEntities.setId(poi, 31L);
        poi.setPurchaseOrder(po);
        poi.setItem(feedItem);
        poi.setQuantityOrdered(100.0);
        poi.setQuantityReceived(0.0);
        poi.setUnitPrice(new BigDecimal("100.00"));

        GoodsReceipt grn = new GoodsReceipt();
        QaEntities.setId(grn, 40L);
        grn.setGrnNumber("GRN-1");
        grn.setPurchaseOrder(po);
        grn.setSupplier(supplier);
        grn.setStatus(GoodsReceiptStatus.DRAFT);

        GoodsReceiptItem gri = new GoodsReceiptItem();
        QaEntities.setId(gri, 41L);
        gri.setGoodsReceipt(grn);
        gri.setPurchaseOrderItem(poi);
        gri.setItem(feedItem);
        gri.setReceivedQty(100.0);
        gri.setUnitCost(new BigDecimal("100.00"));

        when(goodsReceiptRepository.findById(40L)).thenReturn(Optional.of(grn));
        when(goodsReceiptItemRepository.findByGoodsReceiptIdOrderByIdAsc(40L)).thenReturn(List.of(gri));

        service.postGoodsReceipt(40L);
        assertEquals(GoodsReceiptStatus.POSTED, grn.getStatus());
        assertEquals(1, stockInCalls.get());
        assertEquals(100.0, feedItem.getCurrentStock());
        assertEquals(0, new BigDecimal("10000.00").compareTo(finance.balance(AccountCode.ASSET_INVENTORY.getCode())));
        assertEquals(0, new BigDecimal("10000.00").compareTo(finance.balance(AccountCode.LIABILITY_AP.getCode())));
        assertEquals(0L, finance.countByType(TransactionType.INCOME));
        assertEquals(0L, finance.countByType(TransactionType.EXPENSE));

        // Idempotent re-post
        service.postGoodsReceipt(40L);
        assertEquals(1, stockInCalls.get());

        SupplierInvoice inv = new SupplierInvoice();
        QaEntities.setId(inv, 50L);
        inv.setInvoiceNumber("SINV-1");
        inv.setSupplierInvoiceNumber("SUPP-1");
        inv.setSupplier(supplier);
        inv.setPurchaseOrder(po);
        inv.setGoodsReceipt(grn);
        inv.setStatus(SupplierInvoiceStatus.MATCHED);
        inv.setMatchStatus(MatchStatus.MATCHED);
        inv.setTotalAmount(new BigDecimal("10000.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        when(supplierInvoiceRepository.findById(50L)).thenReturn(Optional.of(inv));

        int ledgerBeforeApprove = finance.ledger().size();
        service.approveSupplierInvoice(50L);
        assertEquals(SupplierInvoiceStatus.APPROVED, inv.getStatus());
        assertEquals(ledgerBeforeApprove, finance.ledger().size());

        SupplierPaymentRequest partial = new SupplierPaymentRequest();
        partial.setSupplierInvoiceId(50L);
        partial.setAmount(new BigDecimal("4000.00"));
        partial.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.createPayment(partial);
        assertEquals(SupplierInvoiceStatus.PARTIALLY_PAID, inv.getStatus());
        assertEquals(0, new BigDecimal("6000.00").compareTo(finance.balance(AccountCode.LIABILITY_AP.getCode())));

        SupplierPaymentRequest finalPay = new SupplierPaymentRequest();
        finalPay.setSupplierInvoiceId(50L);
        finalPay.setAmount(new BigDecimal("6000.00"));
        finalPay.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.createPayment(finalPay);
        assertEquals(SupplierInvoiceStatus.PAID, inv.getStatus());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(finance.balance(AccountCode.LIABILITY_AP.getCode())));
        assertEquals(0, new BigDecimal("10000.00").compareTo(finance.balance(AccountCode.ASSET_INVENTORY.getCode())));
        assertEquals(0L, finance.countByType(TransactionType.INCOME));
        finance.assertNoIncomePayments();
    }

    private void setAuth() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "inventory@test.com", "x",
                List.of(new SimpleGrantedAuthority(Permissions.PROCUREMENT_READ),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_CREATE),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_UPDATE),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_APPROVE),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_RECEIVE),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_INVOICE),
                        new SimpleGrantedAuthority(Permissions.PROCUREMENT_PAYMENT)));
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setCurrentUser(User u) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                u.getEmail(), "x",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
