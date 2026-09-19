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
import com.trustagro.materials.dto.MaterialRequestCreateRequest;
import com.trustagro.materials.entity.MaterialRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialRequestRbacFarmScopeTest {

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

    private User farmAManager;
    private Farm farmB;
    private InventoryItem item;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("fm-a@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        farmAManager = new User();
        setId(farmAManager, 1L);
        farmAManager.setEmail("fm-a@test.com");
        farmAManager.setFullName("Farm A Manager");
        farmAManager.setRole(Role.FARM_MANAGER);
        farmAManager.setFarmIds(List.of(1L));

        farmB = new Farm();
        setId(farmB, 2L);
        farmB.setFarmName("Farm B");

        item = new InventoryItem();
        setId(item, 10L);
        item.setItemName("Feed");
        item.setSku("F-1");

        when(farmAccessService.currentUser()).thenReturn(farmAManager);
        lenient().when(farmRepository.findById(2L)).thenReturn(Optional.of(farmB));
        lenient().when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        lenient().when(numberService.nextMaterialRequest()).thenReturn("MR-000099");
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> {
            MaterialRequest mr = inv.getArgument(0);
            if (mr.getId() == null) setId(mr, ids.getAndIncrement());
            return mr;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void farmAManagerCannotCreateForFarmB() {
        doThrow(new BusinessException("Not authorized to access this farm"))
                .when(farmAccessService).assertCanAccessFarm(eq(farmAManager), eq(2L));

        MaterialRequestCreateRequest req = new MaterialRequestCreateRequest();
        req.setType("INTERNAL_ISSUE_REQUEST");
        req.setFarmId(2L);
        req.setReason("Restock");
        MaterialRequestCreateRequest.LineRequest line = new MaterialRequestCreateRequest.LineRequest();
        line.setInventoryItemId(10L);
        line.setQuantityRequested(new BigDecimal("5.000"));
        req.setLines(List.of(line));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().toLowerCase().contains("not authorized"));
        verify(requestRepository, never()).save(any());
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
