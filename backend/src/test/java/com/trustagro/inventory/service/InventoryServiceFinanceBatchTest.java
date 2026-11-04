package com.trustagro.inventory.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinancePostingRequest;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.MovementType;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.entity.StockMovement;
import com.trustagro.inventory.repository.DemandForecastRepository;
import com.trustagro.inventory.repository.InternalRequisitionRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import com.trustagro.notification.repository.NotificationRepository;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceFinanceBatchTest {

    @Mock InventoryItemRepository itemRepository;
    @Mock StockBatchRepository batchRepository;
    @Mock StockMovementRepository movementRepository;
    @Mock PurchaseOrderRepository poRepository;
    @Mock PurchaseOrderItemRepository poItemRepository;
    @Mock DemandForecastRepository forecastRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock FinanceService financeService;
    @Mock UserRepository userRepository;
    @Mock InternalRequisitionRepository requisitionRepository;
    @Mock AuditService auditService;

    @InjectMocks InventoryService service;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem();
        item.setId(10L);
        item.setItemName("Vaccine");
        item.setCurrentStock(5.0);
        item.setAvgUnitCost(new BigDecimal("3.00"));
        lenient().when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        lenient().when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(batchRepository.save(any())).thenAnswer(invocation -> {
            StockBatch batch = invocation.getArgument(0);
            batch.setId(77L);
            return batch;
        });
        lenient().when(movementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movementRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void stockInForFinanceBatchCollectsExistingInventoryApPostingWithoutPostingIndividually() {
        List<FinancePostingRequest> postings = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("batchNumber", "B-1");
        data.put("unitCost", new BigDecimal("20.00"));

        service.stockInForFinanceBatch(10L, 2.0, data, new User(), postings);

        assertEquals(7.0, item.getCurrentStock());
        assertEquals(1, postings.size());
        FinancePostingRequest posting = postings.get(0);
        assertEquals(TransactionType.TRANSFER, posting.transactionType());
        assertEquals(0, new BigDecimal("40.00").compareTo(posting.amount()));
        assertEquals(BusinessUnit.GENERAL, posting.businessUnit());
        assertEquals(AccountCode.ASSET_INVENTORY.getCode(), posting.debitAccountCode());
        assertEquals(AccountCode.LIABILITY_AP.getCode(), posting.creditAccountCode());
        assertEquals("Stock receipt: Vaccine x2.0", posting.description());
        assertEquals(FinanceSourceTypes.MODULE_INVENTORY, posting.sourceModule());
        assertEquals(FinanceSourceTypes.REF_STOCK_IN, posting.referenceType());
        assertEquals(77L, posting.referenceId());
        verify(financeService, never()).postInventoryReceipt(any(), anyString(), anyString(), anyString(), any());
        verify(auditService).log(eq("STOCK_IN"), eq("INVENTORY"), eq("InventoryItem"),
                eq(10L), isNull(), contains("qty=2.0"));
    }

    @Test
    void ordinaryStockInPreservesImmediateFinancePostingBehavior() {
        Map<String, Object> data = new HashMap<>();
        data.put("unitCost", new BigDecimal("20.00"));

        service.stockIn(10L, 2.0, data, new User());

        verify(financeService).postInventoryReceipt(
                eq(new BigDecimal("40.000")), anyString(),
                eq(FinanceSourceTypes.MODULE_INVENTORY),
                eq(FinanceSourceTypes.REF_STOCK_IN), eq(77L));
    }

    @Test
    void stockOutRejectsNonPositiveAndNonFiniteQuantitiesBeforeLoadingInventory() {
        for (double quantity : List.of(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.stockOut(10L, quantity, "ISSUE", null, new User()));
        }

        verify(itemRepository, never()).findById(anyLong());
        verify(batchRepository, never()).findAvailableBatchesFEFO(anyLong(), any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void stockOutAcceptsPositiveFiniteQuantity() {
        StockBatch batch = new StockBatch();
        batch.setId(88L);
        batch.setItem(item);
        batch.setQuantityRemaining(5.0);
        batch.setUnitCost(new BigDecimal("3.00"));
        when(batchRepository.findAvailableBatchesFEFO(eq(10L), any())).thenReturn(List.of(batch));

        Map<String, Object> result = service.stockOut(10L, 2.0, "ISSUE", null, new User());

        assertEquals(3.0, item.getCurrentStock());
        assertEquals(3.0, batch.getQuantityRemaining());
        assertEquals(2.0, result.get("deducted"));
        verify(movementRepository).saveAll(any());
    }

    @Test
    void reversingFarmRecordStockOutRestoresItemAndBatchOnlyOnce() {
        item.setCurrentStock(5.0);
        StockBatch batch = new StockBatch();
        batch.setId(88L);
        batch.setItem(item);
        batch.setQuantityRemaining(2.0);
        StockMovement original = new StockMovement();
        original.setId(500L);
        original.setItem(item);
        original.setBatch(batch);
        original.setMovementType(MovementType.ISSUE);
        original.setQuantity(3.0);
        original.setReferenceType("DAILY_FARM_RECORD");
        original.setReferenceId(50L);

        List<StockMovement> reversals = new ArrayList<>();
        when(movementRepository.findByReferenceTypeAndReferenceId("DAILY_FARM_RECORD", 50L))
                .thenReturn(List.of(original));
        when(movementRepository.findByReferenceTypeAndReferenceIdIn(
                eq("DAILY_FARM_RECORD_REVERSAL"), anyCollection()))
                .thenAnswer(invocation -> reversals);
        when(itemRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(item));
        when(batchRepository.findAllByIdForUpdate(List.of(88L))).thenReturn(List.of(batch));
        when(movementRepository.saveAll(any())).thenAnswer(invocation -> {
            List<StockMovement> saved = invocation.getArgument(0);
            reversals.addAll(saved);
            return saved;
        });

        service.reverseStockOutsForReference(
                "DAILY_FARM_RECORD", 50L, "DAILY_FARM_RECORD_REVERSAL", new User());
        service.reverseStockOutsForReference(
                "DAILY_FARM_RECORD", 50L, "DAILY_FARM_RECORD_REVERSAL", new User());

        assertEquals(8.0, item.getCurrentStock());
        assertEquals(5.0, batch.getQuantityRemaining());
        assertEquals(1, reversals.size());
        assertEquals(MovementType.RETURN, reversals.get(0).getMovementType());
        assertEquals(500L, reversals.get(0).getReferenceId());
        verify(itemRepository, times(1)).save(item);
        verify(batchRepository, times(1)).save(batch);
    }
}
