package com.trustagro.farm.delivery.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.farm.delivery.dto.InternalStockDeliveryRequest;
import com.trustagro.farm.delivery.entity.InternalDeliveryStatus;
import com.trustagro.farm.delivery.entity.InternalStockDelivery;
import com.trustagro.farm.delivery.repository.InternalStockDeliveryRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.farm.service.FarmNumberService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalStockDeliveryServiceTest {

    @Mock InternalStockDeliveryRepository deliveryRepository;
    @Mock InventoryItemRepository itemRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryService inventoryService;
    @Mock FarmNumberService farmNumberService;
    @Mock FarmAccessService farmAccessService;
    @Mock AuditService auditService;

    @InjectMocks InternalStockDeliveryService service;

    private User issuer;
    private InventoryItem item;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("ops@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        issuer = new User();
        issuer.setEmail("ops@test.com");
        issuer.setFullName("Ops");
        setId(issuer, 1L);

        item = new InventoryItem();
        item.setSku("FEED-1");
        item.setItemName("Starter");
        setId(item, 10L);

        when(farmAccessService.currentUser()).thenReturn(issuer);
        when(farmNumberService.nextInternalDeliveryReceipt()).thenReturn("IDR-000001");
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            InternalStockDelivery d = inv.getArgument(0);
            if (d.getId() == null) setId(d, ids.getAndIncrement());
            return d;
        });
        lenient().when(inventoryService.transferStock(any(), any(), any(), any(), any()))
                .thenReturn(Map.of("status", "SUCCESS", "moved", 5.0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void issueTransfersStockExactlyOnceAndNoRevenue() {
        InternalStockDeliveryRequest req = new InternalStockDeliveryRequest();
        req.setInventoryItemId(10L);
        req.setQuantity(new BigDecimal("5"));
        req.setSourceLocation("WAREHOUSE");
        req.setDestinationLocation("FARM-A");
        req.setDeliveryDate(LocalDate.now());

        var created = service.create(req);
        InternalStockDelivery draft = new InternalStockDelivery();
        setId(draft, created.getId());
        draft.setReceiptNumber("IDR-000001");
        draft.setInventoryItem(item);
        draft.setQuantity(new BigDecimal("5"));
        draft.setSourceLocation("WAREHOUSE");
        draft.setDestinationLocation("FARM-A");
        draft.setStatus(InternalDeliveryStatus.APPROVED);
        draft.setCreatedBy(issuer);

        when(deliveryRepository.findById(created.getId())).thenReturn(Optional.of(draft));

        service.issue(created.getId());
        verify(inventoryService, times(1)).transferStock(eq(10L), eq("WAREHOUSE"), eq("FARM-A"), eq(5.0), eq(issuer));
        assertEquals(InternalDeliveryStatus.ISSUED, draft.getStatus());
        assertNotNull(draft.getStockMovementId());

        // Retry issue after already ISSUED should fail transition; re-set and call again with stockMovementId set
        draft.setStatus(InternalDeliveryStatus.APPROVED);
        service.issue(created.getId());
        verify(inventoryService, times(1)).transferStock(any(), any(), any(), any(), any());
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
