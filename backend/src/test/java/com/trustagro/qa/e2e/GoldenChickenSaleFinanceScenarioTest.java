package com.trustagro.qa.e2e;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.BarnRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.sale.dto.ChickenSaleItemRequest;
import com.trustagro.farm.sale.dto.ChickenSalePaymentRequest;
import com.trustagro.farm.sale.dto.ChickenSaleRequest;
import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import com.trustagro.farm.sale.entity.ChickenSaleType;
import com.trustagro.farm.sale.entity.PricingMethod;
import com.trustagro.farm.sale.repository.ChickenSalePaymentRepository;
import com.trustagro.farm.sale.repository.ChickenSaleRepository;
import com.trustagro.farm.sale.repository.FlockPopulationEventRepository;
import com.trustagro.farm.sale.service.ChickenSaleService;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.service.FlockCOGSService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.qa.fixture.QaEntities;
import com.trustagro.qa.fixture.QaFinanceHarness;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.service.WithdrawalService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoldenChickenSaleFinanceScenarioTest {

    @Mock ChickenSaleRepository saleRepository;
    @Mock ChickenSalePaymentRepository paymentRepository;
    @Mock FlockPopulationEventRepository populationEventRepository;
    @Mock FlockRepository flockRepository;
    @Mock FarmRepository farmRepository;
    @Mock BarnRepository barnRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock UserRepository userRepository;
    @Mock WithdrawalService withdrawalService;
    @Mock FlockCOGSService flockCOGSService;
    @Mock NotificationService notificationService;

    private QaFinanceHarness finance;
    private ChickenSaleService service;
    private final AtomicLong ids = new AtomicLong(1);
    private Farm farm;
    private Flock flock;
    private CrmClient customer;
    private ChickenSale lastSale;

    @BeforeEach
    void setUp() {
        finance = new QaFinanceHarness();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("farm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = QaEntities.user(1L, "farm@test.com", "Farm Manager", Role.FARM_MANAGER);
        lenient().when(userRepository.findByEmail("farm@test.com")).thenReturn(Optional.of(user));

        farm = QaEntities.farm(1L, "Main Farm");
        flock = QaEntities.flock(10L, farm, "B-100", 100);
        customer = QaEntities.client(5L, "Abebe Poultry");

        lenient().when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        lenient().when(flockRepository.findById(10L)).thenReturn(Optional.of(flock));
        lenient().when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));
        lenient().when(clientRepository.findById(5L)).thenReturn(Optional.of(customer));
        lenient().when(saleRepository.save(any())).thenAnswer(inv -> {
            ChickenSale s = inv.getArgument(0);
            if (s.getId() == null) QaEntities.setId(s, ids.getAndIncrement());
            lastSale = s;
            return s;
        });
        lenient().when(saleRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(lastSale));
        lenient().when(finance.invoiceRepository().save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) QaEntities.setId(i, ids.getAndIncrement());
            return i;
        });
        lenient().when(finance.invoiceRepository().findBySourceModuleAndSourceId(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> {
            Object p = inv.getArgument(0);
            QaEntities.setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(populationEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(flockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paymentRepository.findBySaleIdOrderByPaymentDateAsc(any())).thenReturn(List.of());
        lenient().when(populationEventRepository.findByChickenSaleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(flockCOGSService.calculateCOGS(any(), any())).thenReturn(new HashMap<>());

        service = new ChickenSaleService(
                saleRepository, paymentRepository, populationEventRepository, flockRepository,
                farmRepository, barnRepository, clientRepository, userRepository,
                finance.invoiceRepository(), finance.finance(), withdrawalService,
                flockCOGSService, finance.auditService(), notificationService);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void golden_chickenSale_confirm_invoice_partialFinalPay_noDoubleRevenue() {
        var created = service.create(validRequest(20, "750"));
        assertEquals(ChickenSaleStatus.DRAFT, created.getStatus());
        assertEquals(100, flock.getCurrentBirdCount());

        service.confirm(lastSale.getId());
        assertEquals(80, flock.getCurrentBirdCount());
        assertEquals(ChickenSaleStatus.CONFIRMED, lastSale.getStatus());

        service.invoice(lastSale.getId());
        assertEquals(ChickenSaleStatus.INVOICED, lastSale.getStatus());
        BigDecimal subtotal = lastSale.getSubtotal();
        assertEquals(1L, finance.countByType(TransactionType.INCOME));
        assertEquals(0, subtotal.compareTo(finance.sumByType(TransactionType.INCOME)));
        assertEquals(0, subtotal.compareTo(finance.balance(AccountCode.REV_POULTRY_SALES.getCode())));

        // Duplicate invoice blocked
        assertThrows(Exception.class, () -> service.invoice(lastSale.getId()));
        assertEquals(1L, finance.countByType(TransactionType.INCOME));

        BigDecimal half = lastSale.getTotalAmount().divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
        ChickenSalePaymentRequest p1 = new ChickenSalePaymentRequest();
        p1.setAmount(half);
        p1.setPaymentMethod(PaymentMethod.CASH);
        service.recordPayment(lastSale.getId(), p1);

        BigDecimal remainder = lastSale.getTotalAmount().subtract(half);
        ChickenSalePaymentRequest p2 = new ChickenSalePaymentRequest();
        p2.setAmount(remainder);
        p2.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        service.recordPayment(lastSale.getId(), p2);

        assertEquals(0, lastSale.getTotalAmount().compareTo(lastOrderPaid(lastSale)));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(finance.balance(AccountCode.ASSET_RECEIVABLE.getCode())));
        assertEquals(1L, finance.countByType(TransactionType.INCOME));
        finance.assertNoIncomePayments();
        verify(finance.auditService(), atLeastOnce()).log(eq("INVOICE"), eq("CHICKEN_SALE"), any(), any(), any(), any());
        verify(notificationService, atLeastOnce()).createRoleNotification(contains("confirmed"), anyString(), eq(Role.FINANCE));
    }

    private static BigDecimal lastOrderPaid(ChickenSale sale) {
        return sale.getPaidAmount() != null ? sale.getPaidAmount() : BigDecimal.ZERO;
    }

    private ChickenSaleRequest validRequest(int qty, String unitPrice) {
        ChickenSaleItemRequest item = new ChickenSaleItemRequest();
        item.setQuantity(qty);
        item.setPricingMethod(PricingMethod.PER_BIRD);
        item.setUnitPrice(new BigDecimal(unitPrice));

        ChickenSaleRequest req = new ChickenSaleRequest();
        req.setFarmId(1L);
        req.setFlockId(10L);
        req.setCustomerId(5L);
        req.setSaleType(ChickenSaleType.RETAIL);
        req.setItems(List.of(item));
        return req;
    }
}
