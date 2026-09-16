package com.trustagro.qa.e2e;

import com.trustagro.audit.service.AuditService;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.FarmCostAllocation;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.FarmCostAllocationRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.qa.fixture.QaEntities;
import com.trustagro.qa.fixture.QaFinanceHarness;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.dto.DispensePrescriptionRequest;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import com.trustagro.veterinary.service.PrescriptionWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoldenVetPharmacyDispenseScenarioTest {

    @Mock PrescriptionRepository prescriptionRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock StockBatchRepository stockBatchRepository;
    @Mock InventoryService inventoryService;
    @Mock FarmCostAllocationRepository farmCostAllocationRepository;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;
    @Mock FarmRepository farmRepository;
    @Mock FlockRepository flockRepository;

    private QaFinanceHarness finance;
    private PrescriptionWorkflowService service;
    private final AtomicInteger stockOutCalls = new AtomicInteger();
    private Prescription rx;

    @BeforeEach
    void setUp() {
        finance = new QaFinanceHarness();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("pharm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User pharmacist = QaEntities.user(3L, "pharm@test.com", "Pharmacist", Role.PHARMACIST);
        lenient().when(userRepository.findByEmail("pharm@test.com")).thenReturn(Optional.of(pharmacist));

        rx = new Prescription();
        QaEntities.setId(rx, 70L);
        rx.setPrescriptionNumber("RX-70");
        rx.setStatus(PrescriptionStatus.APPROVED);
        rx.setInventoryItemId(40L);
        rx.setQuantity(10.0);
        rx.setDrugName("Amox");
        rx.setFarmId(1L);
        rx.setFlockId(2L);

        when(prescriptionRepository.findById(70L)).thenReturn(Optional.of(rx));
        when(prescriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(farmCostAllocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryService.stockOut(any())).thenAnswer(inv -> {
            stockOutCalls.incrementAndGet();
            Map<String, Object> result = new HashMap<>();
            result.put("totalCost", new BigDecimal("200.00"));
            result.put("unitCost", new BigDecimal("20.00"));
            result.put("batchId", 9L);
            result.put("batchNumber", "LOT-V");
            return result;
        });
        lenient().when(stockBatchRepository.findById(9L)).thenReturn(Optional.empty());

        service = new PrescriptionWorkflowService(
                prescriptionRepository, inventoryItemRepository, stockBatchRepository,
                inventoryService, finance.finance(), farmCostAllocationRepository,
                notificationService, finance.auditService(), userRepository,
                farmRepository, flockRepository);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void golden_dispenseOnce_consumptionExpense_notRevenue_duplicateBlocked() {
        DispensePrescriptionRequest req = new DispensePrescriptionRequest();
        service.dispense(70L, req);

        assertEquals(PrescriptionStatus.COMPLETED, rx.getStatus());
        assertEquals(1, stockOutCalls.get());
        assertEquals(0L, finance.countByType(TransactionType.INCOME));
        assertEquals(1L, finance.countByType(TransactionType.EXPENSE));
        assertEquals(0, new BigDecimal("200.00").compareTo(finance.sumByType(TransactionType.EXPENSE)));
        // Dr expense / Cr inventory
        assertEquals(0, new BigDecimal("200.00").compareTo(finance.balance("5120")));
        assertEquals(0, new BigDecimal("-200.00").compareTo(finance.balance(AccountCode.ASSET_INVENTORY.getCode())));
        verify(farmCostAllocationRepository).save(any(FarmCostAllocation.class));

        assertThrows(Exception.class, () -> service.dispense(70L, req));
        assertEquals(1, stockOutCalls.get());
        assertEquals(1L, finance.countByType(TransactionType.EXPENSE));

        // Finance idempotency if somehow re-posted
        finance.finance().postInventoryConsumption(
                new BigDecimal("200.00"),
                com.trustagro.finance.entity.BusinessUnit.PHARMACY,
                "5120", "retry",
                FinanceSourceTypes.MODULE_VETERINARY,
                FinanceSourceTypes.REF_VET_DISPENSE,
                70L);
        assertEquals(1L, finance.countByType(TransactionType.EXPENSE));
    }
}
