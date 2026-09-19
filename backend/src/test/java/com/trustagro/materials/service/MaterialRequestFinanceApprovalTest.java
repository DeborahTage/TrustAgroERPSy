package com.trustagro.materials.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.materials.dto.FinanceDecisionRequest;
import com.trustagro.materials.entity.MaterialRequest;
import com.trustagro.materials.entity.MaterialRequestLine;
import com.trustagro.materials.entity.MaterialRequestStatus;
import com.trustagro.materials.entity.MaterialRequestType;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialRequestFinanceApprovalTest {

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

    private User requester;
    private User financeUser;
    private MaterialRequest request;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("finance@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        requester = user(Role.PHARMACIST, "req@test.com", "Requester");
        financeUser = user(Role.FINANCE, "finance@test.com", "Finance");

        InventoryItem item = new InventoryItem();
        setId(item, 10L);
        item.setItemName("Feed");
        item.setSku("FEED-1");

        request = new MaterialRequest();
        setId(request, 100L);
        request.setRequestNumber("MR-000001");
        request.setType(MaterialRequestType.PURCHASE_REQUEST);
        request.setStatus(MaterialRequestStatus.FINANCE_REVIEW);
        request.setRequester(requester);
        request.setRequestDate(LocalDate.now());
        request.setEstimatedTotal(new BigDecimal("500.00"));
        MaterialRequestLine line = new MaterialRequestLine();
        setId(line, 1L);
        line.setInventoryItem(item);
        line.setQuantityRequested(new BigDecimal("10.000"));
        request.addLine(line);

        when(farmAccessService.currentUser()).thenReturn(financeUser);
        lenient().when(requestRepository.findById(100L)).thenReturn(Optional.of(request));
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void financeApproveDoesNotTouchStockOrPayment() {
        FinanceDecisionRequest body = new FinanceDecisionRequest();
        body.setApprovedAmount(new BigDecimal("500.00"));

        var resp = service.financeApprove(100L, body);

        assertEquals(MaterialRequestStatus.APPROVED.name(), resp.getStatus());
        assertEquals(0, new BigDecimal("500.00").compareTo(resp.getApprovedAmount()));
        verify(inventoryService, never()).stockIn(any(), any(), any(), any());
        verify(inventoryService, never()).stockOut(any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(financeService, never()).postExpense(any(), any(), any(), any(), any(), any(), any());
        verify(notificationService).createUserNotification(anyString(), anyString(), eq(requester));
    }

    @Test
    void financeRejectRequiresReason() {
        FinanceDecisionRequest body = new FinanceDecisionRequest();
        assertThrows(BusinessException.class, () -> service.financeReject(100L, body));

        body.setReason("Insufficient budget");
        var resp = service.financeReject(100L, body);
        assertEquals(MaterialRequestStatus.REJECTED.name(), resp.getStatus());
        assertEquals("Insufficient budget", resp.getRejectionReason());
    }

    @Test
    void selfApproveFailsSoD() {
        when(farmAccessService.currentUser()).thenReturn(requester);
        FinanceDecisionRequest body = new FinanceDecisionRequest();
        body.setApprovedAmount(new BigDecimal("100"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.financeApprove(100L, body));
        assertTrue(ex.getMessage().toLowerCase().contains("own"));
        verify(inventoryService, never()).stockIn(any(), any(), any(), any());
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
