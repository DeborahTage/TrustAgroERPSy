package com.trustagro.qa.e2e;

import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.pharmacy.dto.PosSaleRequest;
import com.trustagro.pharmacy.dto.SaleItemDto;
import com.trustagro.pharmacy.entity.PharmacySale;
import com.trustagro.pharmacy.repository.*;
import com.trustagro.pharmacy.service.PharmacyService;
import com.trustagro.qa.fixture.QaEntities;
import com.trustagro.qa.fixture.QaFinanceHarness;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class GoldenPharmacyFinanceScenarioTest {

    @Mock PharmacyPatientRepository patientRepository;
    @Mock PharmacyPrescriptionRepository rxRepository;
    @Mock PrescriptionItemRepository rxItemRepository;
    @Mock PharmacySaleRepository saleRepository;
    @Mock DurAlertRepository durAlertRepository;
    @Mock PharmacyLabelRepository labelRepository;
    @Mock PharmacyCustomerRepository customerRepository;
    @Mock PharmacySaleItemRepository saleItemRepository;
    @Mock PrescriptionRepository vetRxRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock StockBatchRepository stockBatchRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock InventoryService inventoryService;
    @Mock NotificationService notificationService;

    private QaFinanceHarness finance;
    private PharmacyService service;
    private InventoryItem drug;
    private final AtomicLong ids = new AtomicLong(1);
    private final AtomicInteger stockOutCalls = new AtomicInteger();

    @BeforeEach
    void setUp() {
        finance = new QaFinanceHarness();
        drug = QaEntities.item(40L, "DRUG-001", "Amoxicillin", 100.0, new BigDecimal("20.00"));

        when(inventoryItemRepository.findById(40L)).thenReturn(Optional.of(drug));
        when(inventoryService.stockOut(anyLong(), anyDouble(), eq("SALE"), any(), any())).thenAnswer(inv -> {
            stockOutCalls.incrementAndGet();
            double qty = inv.getArgument(1);
            drug.setCurrentStock(drug.getCurrentStock() - qty);
            Map<String, Object> result = new HashMap<>();
            result.put("totalCost", new BigDecimal("20.00").multiply(BigDecimal.valueOf(qty)));
            result.put("unitCost", new BigDecimal("20.00"));
            result.put("batchNumber", "LOT-1");
            return result;
        });
        when(saleRepository.save(any())).thenAnswer(inv -> {
            PharmacySale s = inv.getArgument(0);
            if (s.getId() == null) QaEntities.setId(s, ids.getAndIncrement());
            return s;
        });
        when(finance.invoiceRepository().save(any())).thenAnswer(inv -> {
            var i = inv.getArgument(0);
            QaEntities.setId(i, ids.getAndIncrement());
            return i;
        });

        service = new PharmacyService(
                patientRepository, rxRepository, rxItemRepository, saleRepository,
                durAlertRepository, labelRepository, customerRepository, saleItemRepository,
                vetRxRepository, inventoryItemRepository, stockBatchRepository, stockMovementRepository,
                finance.finance(), inventoryService, notificationService, finance.invoiceRepository());
    }

    @Test
    void golden_pharmacyCashSale_stockOnce_revenueOnce_cogsOnce_paymentNotIncome() {
        User pharmacist = QaEntities.user(2L, "pharm@test.com", "Pharmacist", Role.PHARMACIST);

        SaleItemDto line = new SaleItemDto();
        line.setItemId(40L);
        line.setQty(new BigDecimal("5"));
        line.setUnitPrice(new BigDecimal("30.00"));

        PosSaleRequest req = new PosSaleRequest();
        req.setItems(List.of(line));
        req.setCustomerName("Walk-in");
        req.setPaymentMethod("CASH");

        PharmacySale sale = service.processSale(req, pharmacist);
        assertNotNull(sale.getId());
        assertEquals(95.0, drug.getCurrentStock());
        assertEquals(1, stockOutCalls.get());

        // subtotal 150, VAT 22.50, COGS 100
        assertEquals(0, new BigDecimal("150.00").compareTo(finance.sumByType(TransactionType.INCOME)));
        assertEquals(1L, finance.countByType(TransactionType.INCOME));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                finance.ledger().stream()
                        .filter(t -> "PHARMACY_SALE_COGS".equals(t.getReferenceType())
                                || (t.getReferenceType() != null && t.getReferenceType().contains("COGS")))
                        .map(t -> t.getAmount())
                        .findFirst()
                        .orElseGet(() -> finance.byModule("PHARMACY").stream()
                                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                                .map(t -> t.getAmount())
                                .findFirst()
                                .orElse(BigDecimal.ZERO))));
        assertEquals(0, new BigDecimal("150.00").compareTo(finance.balance(AccountCode.REV_PHARMACY_DRUGS.getCode())));
        // Cash sale: cash increased by subtotal+VAT, AR untouched
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(finance.balance(AccountCode.ASSET_RECEIVABLE.getCode())));
        assertTrue(finance.balance(AccountCode.ASSET_CASH.getCode()).compareTo(BigDecimal.ZERO) > 0);
        finance.assertNoIncomePayments();

        // Retry same sale path would stock out again at POS level — inventory mock proves single call for one sale
        verify(inventoryService, times(1)).stockOut(eq(40L), eq(5.0), eq("SALE"), any(), any());
        verify(notificationService, atLeastOnce()).createRoleNotification(contains("Sale"), anyString(), eq(Role.FINANCE));
    }
}
