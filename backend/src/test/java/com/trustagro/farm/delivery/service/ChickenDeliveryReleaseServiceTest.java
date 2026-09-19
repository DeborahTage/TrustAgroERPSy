package com.trustagro.farm.delivery.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.farm.delivery.dto.ChickenDispatchRequest;
import com.trustagro.farm.delivery.dto.ChickenDeliveryReleaseRequest;
import com.trustagro.farm.delivery.entity.ChickenDeliveryRelease;
import com.trustagro.farm.delivery.entity.ChickenReleaseStatus;
import com.trustagro.farm.delivery.repository.ChickenDeliveryReleaseRepository;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSaleItem;
import com.trustagro.farm.sale.entity.FlockPopulationEventType;
import com.trustagro.farm.sale.repository.ChickenSalePaymentRepository;
import com.trustagro.farm.sale.repository.ChickenSaleRepository;
import com.trustagro.farm.sale.repository.FlockPopulationEventRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.farm.service.FarmNumberService;
import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.notification.service.NotificationService;
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
class ChickenDeliveryReleaseServiceTest {

    @Mock ChickenDeliveryReleaseRepository releaseRepository;
    @Mock ChickenSaleRepository saleRepository;
    @Mock ChickenSalePaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock FlockRepository flockRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock FlockPopulationEventRepository populationEventRepository;
    @Mock FarmNumberService farmNumberService;
    @Mock FarmAccessService farmAccessService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks ChickenDeliveryReleaseService service;

    private User farmManager;
    private Farm farm;
    private Flock flock;
    private ChickenSale sale;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("fm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        farmManager = new User();
        farmManager.setEmail("fm@test.com");
        farmManager.setFullName("Farm Manager");
        farmManager.setRole(Role.FARM_MANAGER);
        setId(farmManager, 1L);

        farm = new Farm();
        farm.setFarmName("Main");
        setId(farm, 1L);

        flock = new Flock();
        flock.setBatchCode("B-1");
        flock.setFarm(farm);
        flock.setBirdType("Broiler");
        flock.setCurrentBirdCount(100);
        flock.setStatus(FlockStatus.ACTIVE);
        setId(flock, 10L);

        CrmClient customer = new CrmClient();
        customer.setClientName("Buyer");
        setId(customer, 5L);

        Invoice invoice = new Invoice();
        setId(invoice, 50L);

        sale = new ChickenSale();
        setId(sale, 20L);
        sale.setSaleNumber("CS-1");
        sale.setFarm(farm);
        sale.setFlock(flock);
        sale.setCustomer(customer);
        sale.setInvoice(invoice);
        sale.setPaidAmount(new BigDecimal("1000.00"));
        ChickenSaleItem item = new ChickenSaleItem();
        item.setQuantity(50);
        item.setSale(sale);
        sale.getItems().add(item);

        when(farmAccessService.currentUser()).thenReturn(farmManager);
        lenient().doNothing().when(farmAccessService).assertCanAccessFarm(any(), any());
        lenient().when(farmAccessService.hasGlobalFarmAccess(any())).thenReturn(true);
        lenient().when(farmNumberService.nextPaymentVerification()).thenReturn("PV-000001");
        lenient().when(releaseRepository.save(any())).thenAnswer(inv -> {
            ChickenDeliveryRelease r = inv.getArgument(0);
            if (r.getId() == null) setId(r, ids.getAndIncrement());
            return r;
        });
        lenient().when(releaseRepository.sumAuthorizedForSale(20L)).thenReturn(0);
        lenient().when(populationEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(flockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dispatchDecrementsOnceWithBonusAndNoFinanceRevenue() {
        ChickenDeliveryRelease release = approvedRelease(30, 5);
        when(releaseRepository.findById(release.getId())).thenReturn(Optional.of(release));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));

        ChickenDispatchRequest req = new ChickenDispatchRequest();
        req.setDispatchDate(LocalDate.now());
        req.setChickenType("Broiler");
        req.setBonusQuantity(5);

        service.dispatch(release.getId(), req);

        assertEquals(65, flock.getCurrentBirdCount());
        assertEquals(FlockStatus.PARTIALLY_SOLD, flock.getStatus());
        assertTrue(release.isStockDecremented());
        assertEquals(ChickenReleaseStatus.DISPATCHED, release.getStatus());
        verify(populationEventRepository).save(argThat(e ->
                e.getEventType() == FlockPopulationEventType.DISPATCH && e.getQuantityChange() == 35));

        // Second dispatch attempt should fail on status
        assertThrows(BusinessException.class, () -> service.dispatch(release.getId(), req));
        assertEquals(65, flock.getCurrentBirdCount());
    }

    @Test
    void dispatchRejectsWrongFarmAccess() {
        ChickenDeliveryRelease release = approvedRelease(10, 0);
        when(releaseRepository.findById(release.getId())).thenReturn(Optional.of(release));
        doThrow(new BusinessException("Not authorized to access this farm"))
                .when(farmAccessService).assertCanAccessFarm(eq(farmManager), eq(1L));

        ChickenDispatchRequest req = new ChickenDispatchRequest();
        req.setDispatchDate(LocalDate.now());
        req.setChickenType("Broiler");

        assertThrows(BusinessException.class, () -> service.dispatch(release.getId(), req));
        verify(flockRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void dispatchRejectsInsufficientBirds() {
        flock.setCurrentBirdCount(10);
        ChickenDeliveryRelease release = approvedRelease(20, 5);
        when(releaseRepository.findById(release.getId())).thenReturn(Optional.of(release));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));

        ChickenDispatchRequest req = new ChickenDispatchRequest();
        req.setDispatchDate(LocalDate.now());
        req.setChickenType("Broiler");
        req.setBonusQuantity(5);

        assertThrows(BusinessException.class, () -> service.dispatch(release.getId(), req));
        assertFalse(release.isStockDecremented());
    }

    @Test
    void createRequiresPaidInvoiceOrPayment() {
        sale.setInvoice(null);
        sale.setPaidAmount(BigDecimal.ZERO);
        when(saleRepository.findById(20L)).thenReturn(Optional.of(sale));

        ChickenDeliveryReleaseRequest req = new ChickenDeliveryReleaseRequest();
        req.setChickenSaleId(20L);
        req.setAuthorizedChickenQuantity(10);

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    private ChickenDeliveryRelease approvedRelease(int authorized, int bonus) {
        ChickenDeliveryRelease r = new ChickenDeliveryRelease();
        setId(r, ids.getAndIncrement());
        r.setReleaseNumber("PV-1");
        r.setChickenSale(sale);
        r.setFarm(farm);
        r.setFlock(flock);
        r.setCustomer(sale.getCustomer());
        r.setAuthorizedChickenQuantity(authorized);
        r.setBonusQuantity(bonus);
        r.setChickenType("Broiler");
        r.setStatus(ChickenReleaseStatus.FARM_APPROVED);
        r.setStockDecremented(false);
        return r;
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
