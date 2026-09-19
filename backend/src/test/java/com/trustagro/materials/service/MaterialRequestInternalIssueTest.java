package com.trustagro.materials.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.materials.dto.AcknowledgeRequest;
import com.trustagro.materials.dto.FinanceDecisionRequest;
import com.trustagro.materials.dto.IssueRequest;
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
class MaterialRequestInternalIssueTest {

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

    private User farmManager;
    private User financeUser;
    private User storeUser;
    private Farm farm;
    private InventoryItem item;
    private MaterialRequest request;
    private MaterialRequestLine line;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("fm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        farmManager = user(Role.FARM_MANAGER, "fm@test.com", "Farm Mgr");
        financeUser = user(Role.FINANCE, "fin@test.com", "Finance");
        storeUser = user(Role.INVENTORY_MANAGER, "store@test.com", "Store");

        farm = new Farm();
        setId(farm, 1L);
        farm.setFarmName("Farm A");

        item = new InventoryItem();
        setId(item, 10L);
        item.setItemName("Feed Bags");
        item.setSku("FEED-BAG");
        item.setCurrentStock(100.0);
        item.setAvgUnitCost(new BigDecimal("5.00"));

        request = new MaterialRequest();
        setId(request, 300L);
        request.setRequestNumber("MR-000300");
        request.setType(MaterialRequestType.INTERNAL_ISSUE_REQUEST);
        request.setStatus(MaterialRequestStatus.FINANCE_REVIEW);
        request.setRequester(farmManager);
        request.setFarm(farm);
        request.setRequestDate(LocalDate.now());
        request.setEstimatedTotal(BigDecimal.ZERO);

        line = new MaterialRequestLine();
        setId(line, 31L);
        line.setInventoryItem(item);
        line.setQuantityRequested(new BigDecimal("100.000"));
        line.setQuantityApproved(BigDecimal.ZERO);
        line.setQuantityIssued(BigDecimal.ZERO);
        request.addLine(line);

        when(farmAccessService.currentUser()).thenReturn(financeUser);
        lenient().doNothing().when(farmAccessService).assertCanAccessFarm(any(), any());
        lenient().when(requestRepository.findById(300L)).thenReturn(Optional.of(request));
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(numberService.nextStoreIssue()).thenReturn("SI-000001");
        lenient().when(issueEventRepository.save(any())).thenAnswer(inv -> {
            MaterialIssueEvent e = inv.getArgument(0);
            if (e.getId() == null) setId(e, ids.getAndIncrement());
            return e;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveIssuePartialThenAcknowledgeNoSecondDecrement() {
        // finance approve → READY_FOR_STORE
        FinanceDecisionRequest approve = new FinanceDecisionRequest();
        var approved = service.financeApprove(300L, approve);
        assertEquals(MaterialRequestStatus.READY_FOR_STORE.name(), approved.getStatus());
        assertEquals(0, new BigDecimal("100.000").compareTo(request.getLines().get(0).getQuantityApproved()));
        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), anyLong(), any());

        // store issues 30 of 100
        when(farmAccessService.currentUser()).thenReturn(storeUser);
        Map<String, Object> stockResult = new HashMap<>();
        stockResult.put("batchId", 1L);
        stockResult.put("newStock", 70.0);
        stockResult.put("totalCost", new BigDecimal("150.00"));
        when(inventoryService.stockOut(eq(10L), eq(30.0), eq("MATERIAL_ISSUE"), eq(300L), eq(storeUser),
                isNull(), anyString(), eq("INTERNAL_CONSUMPTION"))).thenAnswer(inv -> {
            item.setCurrentStock(70.0);
            return stockResult;
        });

        IssueRequest issueReq = new IssueRequest();
        IssueRequest.LineIssue li = new IssueRequest.LineIssue();
        li.setLineId(31L);
        li.setQuantityIssued(new BigDecimal("30.000"));
        issueReq.setLines(List.of(li));

        var issued = service.issueInternal(300L, issueReq);
        assertEquals(MaterialRequestStatus.PARTIALLY_FULFILLED.name(), issued.getStatus());
        assertEquals(0, new BigDecimal("30.000").compareTo(issued.getLines().get(0).getQuantityIssued()));
        assertEquals(70.0, item.getCurrentStock());
        verify(inventoryService, times(1)).stockOut(eq(10L), eq(30.0), eq("MATERIAL_ISSUE"), eq(300L),
                eq(storeUser), isNull(), anyString(), eq("INTERNAL_CONSUMPTION"));

        // acknowledge — no further stock out
        when(farmAccessService.currentUser()).thenReturn(farmManager);
        AcknowledgeRequest ack = new AcknowledgeRequest();
        ack.setNotes("Received on farm");
        service.acknowledgeReceipt(300L, ack);
        verify(inventoryService, times(1)).stockOut(anyLong(), anyDouble(), anyString(), anyLong(),
                any(), any(), any(), any());

        // fulfill remaining 70
        when(farmAccessService.currentUser()).thenReturn(storeUser);
        request.setStatus(MaterialRequestStatus.PARTIALLY_FULFILLED);
        Map<String, Object> stock2 = new HashMap<>();
        stock2.put("batchId", 2L);
        stock2.put("newStock", 0.0);
        stock2.put("totalCost", new BigDecimal("350.00"));
        when(inventoryService.stockOut(eq(10L), eq(70.0), eq("MATERIAL_ISSUE"), eq(300L), eq(storeUser),
                isNull(), anyString(), eq("INTERNAL_CONSUMPTION"))).thenAnswer(inv -> {
            item.setCurrentStock(0.0);
            return stock2;
        });
        when(numberService.nextStoreIssue()).thenReturn("SI-000002");

        IssueRequest issue2 = new IssueRequest();
        IssueRequest.LineIssue li2 = new IssueRequest.LineIssue();
        li2.setLineId(31L);
        li2.setQuantityIssued(new BigDecimal("70.000"));
        issue2.setLines(List.of(li2));

        var fulfilled = service.issueInternal(300L, issue2);
        assertEquals(MaterialRequestStatus.ISSUED.name(), fulfilled.getStatus());
        assertEquals(0, new BigDecimal("100.000").compareTo(fulfilled.getLines().get(0).getQuantityIssued()));
        assertEquals(0.0, item.getCurrentStock());
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
