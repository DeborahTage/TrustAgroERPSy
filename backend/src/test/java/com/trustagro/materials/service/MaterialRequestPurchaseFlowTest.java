package com.trustagro.materials.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.materials.dto.DisbursementRequest;
import com.trustagro.materials.dto.GoodsReceiptRequest;
import com.trustagro.materials.entity.*;
import com.trustagro.materials.repository.MaterialGoodsReceiptRepository;
import com.trustagro.materials.repository.MaterialIssueEventRepository;
import com.trustagro.materials.repository.MaterialRequestDisbursementRepository;
import com.trustagro.materials.repository.MaterialRequestRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.service.ProcNumberService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialRequestPurchaseFlowTest {

    @Mock MaterialRequestRepository requestRepository;
    @Mock MaterialRequestDisbursementRepository disbursementRepository;
    @Mock MaterialGoodsReceiptRepository goodsReceiptRepository;
    @Mock MaterialIssueEventRepository issueEventRepository;
    @Mock InventoryItemRepository itemRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock FarmRepository farmRepository;
    @Mock InventoryService inventoryService;
    @Mock FinanceService financeService;
    @Mock ProcurementService procurementService;
    @Mock ProcNumberService numberService;
    @Mock FarmAccessService farmAccessService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks MaterialRequestService service;

    private User financeUser;
    private User storeUser;
    private MaterialRequest request;
    private MaterialRequestLine line;
    private Supplier supplier;
    private InventoryItem item;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("fin@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        financeUser = user(Role.FINANCE, "fin@test.com", "Finance");
        storeUser = user(Role.INVENTORY_MANAGER, "store@test.com", "Store");

        item = new InventoryItem();
        setId(item, 10L);
        item.setItemName("Vaccine");
        item.setSku("VAC-1");
        item.setCurrentStock(0.0);

        supplier = new Supplier();
        setId(supplier, 5L);
        supplier.setName("Acme");

        User requester = user(Role.PHARMACIST, "ph@test.com", "Pharm");
        request = new MaterialRequest();
        setId(request, 200L);
        request.setRequestNumber("MR-000200");
        request.setType(MaterialRequestType.PURCHASE_REQUEST);
        request.setStatus(MaterialRequestStatus.APPROVED);
        request.setRequester(requester);
        request.setPreferredSupplier(supplier);
        request.setRequestDate(LocalDate.now());
        request.setEstimatedTotal(new BigDecimal("1000.00"));
        request.setApprovedAmount(new BigDecimal("1000.00"));
        request.setPaidAmount(BigDecimal.ZERO);

        line = new MaterialRequestLine();
        setId(line, 21L);
        line.setInventoryItem(item);
        line.setQuantityRequested(new BigDecimal("50.000"));
        line.setQuantityApproved(new BigDecimal("50.000"));
        line.setQuantityOrdered(new BigDecimal("50.000"));
        line.setQuantityReceived(BigDecimal.ZERO);
        line.setEstimatedUnitPrice(new BigDecimal("20.00"));
        request.addLine(line);

        lenient().when(farmAccessService.currentUser()).thenReturn(financeUser);
        lenient().when(requestRepository.findById(200L)).thenReturn(Optional.of(request));
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
        lenient().when(numberService.nextMaterialDisbursement()).thenReturn("MD-000001");
        lenient().when(numberService.nextMaterialStoreReceipt()).thenReturn("MSR-000001");
        lenient().when(disbursementRepository.save(any())).thenAnswer(inv -> {
            MaterialRequestDisbursement d = inv.getArgument(0);
            if (d.getId() == null) setId(d, ids.getAndIncrement());
            return d;
        });
        lenient().when(goodsReceiptRepository.save(any())).thenAnswer(inv -> {
            MaterialGoodsReceipt r = inv.getArgument(0);
            if (r.getId() == null) setId(r, ids.getAndIncrement());
            return r;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disbursePostsFinanceOnceAndSecondConflicts() {
        FinanceTransaction tx = new FinanceTransaction();
        setId(tx, 900L);
        when(financeService.postTransaction(eq(TransactionType.EXPENSE), any(), any(), any(), any(),
                anyString(), eq(FinanceSourceTypes.MODULE_MATERIALS),
                eq(FinanceSourceTypes.REF_MATERIAL_DISBURSEMENT), anyLong(), any(), any()))
                .thenReturn(tx);
        when(disbursementRepository.existsByMaterialRequestIdAndAmount(eq(200L), any()))
                .thenReturn(false);

        DisbursementRequest body = new DisbursementRequest();
        body.setAmount(new BigDecimal("400.00"));
        body.setSupplierId(5L);
        body.setPaymentMethod("CASH");

        var resp = service.recordDisbursement(200L, body);
        assertEquals(MaterialRequestStatus.PAYMENT_SENT.name(), resp.getStatus());
        assertEquals(0, new BigDecimal("400.00").compareTo(resp.getPaidAmount()));
        verify(financeService, times(1)).postTransaction(eq(TransactionType.EXPENSE), any(), any(), any(), any(),
                anyString(), eq(FinanceSourceTypes.MODULE_MATERIALS),
                eq(FinanceSourceTypes.REF_MATERIAL_DISBURSEMENT), anyLong(), any(), any());
        verify(inventoryService, never()).stockIn(any(), any(), any(), any());

        when(disbursementRepository.existsByMaterialRequestIdAndAmount(eq(200L), eq(new BigDecimal("400.00"))))
                .thenReturn(true);
        DisbursementRequest dup = new DisbursementRequest();
        dup.setAmount(new BigDecimal("400.00"));
        dup.setSupplierId(5L);
        assertThrows(BusinessException.class, () -> service.recordDisbursement(200L, dup));
    }

    @Test
    void disburseIdempotentByNumber() {
        MaterialRequestDisbursement existing = new MaterialRequestDisbursement();
        setId(existing, 55L);
        existing.setDisbursementNumber("MD-FIXED");
        existing.setAmount(new BigDecimal("100.00"));
        when(disbursementRepository.findByDisbursementNumber("MD-FIXED")).thenReturn(Optional.of(existing));

        DisbursementRequest body = new DisbursementRequest();
        body.setDisbursementNumber("MD-FIXED");
        body.setAmount(new BigDecimal("100.00"));
        body.setSupplierId(5L);

        service.recordDisbursement(200L, body);
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(inventoryService, never()).stockIn(any(), any(), any(), any());
    }

    @Test
    void goodsReceiptIncreasesStockOncePaymentDoesNot() {
        when(farmAccessService.currentUser()).thenReturn(storeUser);
        request.setStatus(MaterialRequestStatus.PAYMENT_SENT);
        request.setPaidAmount(new BigDecimal("400.00"));

        Map<String, Object> stockResult = new HashMap<>();
        stockResult.put("batchId", 77L);
        stockResult.put("newStock", 50.0);
        when(inventoryService.stockIn(eq(10L), eq(50.0), anyMap(), eq(storeUser))).thenReturn(stockResult);

        GoodsReceiptRequest body = new GoodsReceiptRequest();
        GoodsReceiptRequest.LineReceipt lr = new GoodsReceiptRequest.LineReceipt();
        lr.setLineId(21L);
        lr.setQuantityReceived(new BigDecimal("50.000"));
        lr.setBatchNumber("B-1");
        lr.setUnitCost(new BigDecimal("20.00"));
        body.setLines(List.of(lr));

        var resp = service.registerGoodsReceipt(200L, body);
        assertEquals(0, new BigDecimal("50.000").compareTo(resp.getLines().get(0).getQuantityReceived()));
        verify(inventoryService, times(1)).stockIn(eq(10L), eq(50.0), anyMap(), eq(storeUser));

        // payment path never stocks
        verify(inventoryService, never()).stockOut(any(), any(), any(), any(), any());
    }

    private User user(Role role, String email, String name) {
        User u = new User();
        setId(u, ids.getAndIncrement());
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        return u;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
