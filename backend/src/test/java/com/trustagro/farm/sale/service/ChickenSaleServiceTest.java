package com.trustagro.farm.sale.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
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
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.FlockCOGSService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.service.WithdrawalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChickenSaleServiceTest {

    @Mock ChickenSaleRepository saleRepository;
    @Mock ChickenSalePaymentRepository paymentRepository;
    @Mock FlockPopulationEventRepository populationEventRepository;
    @Mock FlockRepository flockRepository;
    @Mock FarmRepository farmRepository;
    @Mock BarnRepository barnRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock UserRepository userRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock FinanceService financeService;
    @Mock WithdrawalService withdrawalService;
    @Mock FlockCOGSService flockCOGSService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks ChickenSaleService service;

    private Farm farm;
    private Flock flock;
    private CrmClient customer;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("farm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setEmail("farm@test.com");
        user.setFullName("Farm Manager");
        lenient().when(userRepository.findByEmail("farm@test.com")).thenReturn(Optional.of(user));

        farm = new Farm();
        farm.setFarmName("Main Farm");
        farm.setStatus(FarmStatus.ACTIVE);
        setId(farm, 1L);

        flock = new Flock();
        flock.setBatchCode("B-100");
        flock.setFarm(farm);
        flock.setStatus(FlockStatus.ACTIVE);
        flock.setCurrentBirdCount(100);
        setId(flock, 10L);

        customer = new CrmClient();
        customer.setClientName("Abebe");
        customer.setStatus(ClientStatus.ACTIVE);
        setId(customer, 5L);

        lenient().when(saleRepository.save(any())).thenAnswer(inv -> {
            ChickenSale s = inv.getArgument(0);
            if (s.getId() == null) setId(s, ids.getAndIncrement());
            return s;
        });
        lenient().when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) setId(i, 50L);
            return i;
        });
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0);
            try {
                var f = p.getClass().getDeclaredField("id");
                f.setAccessible(true);
                if (f.get(p) == null) f.set(p, ids.getAndIncrement());
            } catch (Exception ignored) { }
            return p;
        });
        lenient().when(populationEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(flockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paymentRepository.findBySaleIdOrderByPaymentDateAsc(any())).thenReturn(List.of());
        lenient().when(populationEventRepository.findByChickenSaleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        lenient().when(flockCOGSService.calculateCOGS(any(), any())).thenReturn(new HashMap<>());
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    FinanceTransaction tx = new FinanceTransaction();
                    setId(tx, 77L);
                    return tx;
                });
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    FinanceTransaction tx = new FinanceTransaction();
                    setId(tx, 78L);
                    return tx;
                });
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSalePersistsDraftWithCustomerAndFlock() {
        stubLookups();
        var created = service.create(validRequest(20, "750"));
        assertEquals(ChickenSaleStatus.DRAFT, created.getStatus());
        assertEquals(5L, created.getCustomerId());
        assertEquals(10L, created.getFlockId());
        assertEquals(20, created.getTotalBirds());
        assertEquals(0, new BigDecimal("15000.00").compareTo(created.getTotalAmount()));
        verify(auditService).log(eq("CREATE"), eq("CHICKEN_SALE"), eq("ChickenSale"), any(), any(), any());
    }

    @Test
    void rejectsZeroQuantity() {
        stubLookups();
        ChickenSaleRequest req = validRequest(0, "750");
        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void rejectsNegativePrice() {
        stubLookups();
        ChickenSaleRequest req = validRequest(10, "-1");
        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void rejectsInvalidCustomer() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        when(flockRepository.findById(10L)).thenReturn(Optional.of(flock));
        when(clientRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.create(validRequest(10, "100")));
    }

    @Test
    void rejectsInactiveFlock() {
        flock.setStatus(FlockStatus.CLOSED);
        stubLookups();
        assertThrows(BusinessException.class, () -> service.create(validRequest(10, "100")));
    }

    @Test
    void cannotSellMoreThanFlockPopulation() {
        stubLookups();
        assertThrows(BusinessException.class, () -> service.create(validRequest(150, "100")));
    }

    @Test
    void confirmDecrementsFlockPopulation() {
        ChickenSale sale = draftSale(80);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));

        var confirmed = service.confirm(sale.getId());
        assertEquals(ChickenSaleStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(20, flock.getCurrentBirdCount());
        verify(populationEventRepository).save(any());
        verify(auditService).log(eq("CONFIRM"), eq("CHICKEN_SALE"), eq("ChickenSale"), any(), any(), any());
    }

    @Test
    void confirmRejectsOversellUnderLock() {
        ChickenSale sale = draftSale(80);
        flock.setCurrentBirdCount(50);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));
        assertThrows(BusinessException.class, () -> service.confirm(sale.getId()));
        assertEquals(50, flock.getCurrentBirdCount());
    }

    @Test
    void doubleSaleSecondConfirmRejected() {
        ChickenSale first = draftSale(80);
        when(saleRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));
        service.confirm(first.getId());
        assertEquals(20, flock.getCurrentBirdCount());

        ChickenSale second = draftSale(50);
        when(saleRepository.findById(second.getId())).thenReturn(Optional.of(second));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(second.getId()));
        assertTrue(ex.getMessage().contains("has only 20"));
        assertEquals(20, flock.getCurrentBirdCount());
    }

    @Test
    void invoiceCreatesFinanceRecord() {
        ChickenSale sale = draftSale(10);
        sale.setStatus(ChickenSaleStatus.CONFIRMED);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        var invoiced = service.invoice(sale.getId());
        assertEquals(ChickenSaleStatus.INVOICED, invoiced.getStatus());
        assertNotNull(invoiced.getInvoiceNumber());
        verify(financeService).postTransaction(any(), any(), any(), eq("1200"), eq("4100"), any(), eq("CHICKEN_SALE"), eq("INVOICE_REVENUE"), any());
        verify(auditService).log(eq("INVOICE"), eq("CHICKEN_SALE"), eq("ChickenSale"), any(), any(), any());
    }

    @Test
    void partialAndFullPaymentUpdateBalances() {
        ChickenSale sale = draftSale(10);
        sale.setStatus(ChickenSaleStatus.INVOICED);
        Invoice invoice = new Invoice();
        setId(invoice, 50L);
        invoice.setInvoiceNumber("CSINV-1");
        invoice.setTotalAmount(sale.getTotalAmount());
        invoice.setPaidAmount(BigDecimal.ZERO);
        sale.setInvoice(invoice);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        ChickenSalePaymentRequest partial = new ChickenSalePaymentRequest();
        partial.setAmount(new BigDecimal("3000"));
        partial.setPaymentMethod(PaymentMethod.CASH);
        var afterPartial = service.recordPayment(sale.getId(), partial);
        assertEquals(ChickenSaleStatus.PARTIALLY_PAID, afterPartial.getStatus());
        assertEquals(0, new BigDecimal("3000.00").compareTo(afterPartial.getPaidAmount()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(afterPartial.getOutstandingAmount()));
        // Settlement is TRANSFER keyed by payment id — never INCOME
        verify(financeService).postTransaction(eq(com.trustagro.finance.entity.TransactionType.TRANSFER),
                eq(new BigDecimal("3000.00")), any(), eq("1000"), eq("1200"), any(),
                eq("CHICKEN_SALE"), eq("PAYMENT"), any(), eq(PaymentMethod.CASH), any());

        ChickenSalePaymentRequest rest = new ChickenSalePaymentRequest();
        rest.setAmount(new BigDecimal("4500"));
        rest.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        var afterFull = service.recordPayment(sale.getId(), rest);
        assertEquals(ChickenSaleStatus.PAID, afterFull.getStatus());
        assertEquals(0, afterFull.getOutstandingAmount().compareTo(BigDecimal.ZERO.setScale(2)));
        verify(financeService, times(2)).postTransaction(eq(com.trustagro.finance.entity.TransactionType.TRANSFER),
                any(), any(), any(), eq("1200"), any(), eq("CHICKEN_SALE"), eq("PAYMENT"), any(), any(), any());
    }

    @Test
    void seventyFiveThousandInvoiceWithThirtyThousandPaymentLeavesFortyFiveAr() {
        ChickenSale sale = draftSale(100); // 100 * 750 = 75_000
        sale.setStatus(ChickenSaleStatus.INVOICED);
        Invoice invoice = new Invoice();
        setId(invoice, 50L);
        invoice.setTotalAmount(sale.getTotalAmount());
        invoice.setPaidAmount(BigDecimal.ZERO);
        sale.setInvoice(invoice);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));

        ChickenSalePaymentRequest partial = new ChickenSalePaymentRequest();
        partial.setAmount(new BigDecimal("30000"));
        partial.setPaymentMethod(PaymentMethod.CASH);
        var after = service.recordPayment(sale.getId(), partial);

        assertEquals(0, new BigDecimal("75000.00").compareTo(sale.getTotalAmount()));
        assertEquals(0, new BigDecimal("30000.00").compareTo(after.getPaidAmount()));
        assertEquals(0, new BigDecimal("45000.00").compareTo(after.getOutstandingAmount()));
        verify(financeService, never()).postTransaction(eq(com.trustagro.finance.entity.TransactionType.INCOME),
                eq(new BigDecimal("30000.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelConfirmedRestoresPopulation() {
        ChickenSale sale = draftSale(30);
        sale.setStatus(ChickenSaleStatus.CONFIRMED);
        flock.setCurrentBirdCount(70);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));

        var cancelled = service.cancel(sale.getId());
        assertEquals(ChickenSaleStatus.CANCELLED, cancelled.getStatus());
        assertEquals(100, flock.getCurrentBirdCount());
        verify(populationEventRepository).save(any());
    }

    @Test
    void cannotCancelAfterInvoice() {
        ChickenSale sale = draftSale(10);
        sale.setStatus(ChickenSaleStatus.INVOICED);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        assertThrows(BusinessException.class, () -> service.cancel(sale.getId()));
    }

    @Test
    void invalidStatusTransitionRejected() {
        ChickenSale sale = draftSale(10);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        assertThrows(BusinessException.class, () -> service.invoice(sale.getId()));
    }

    @Test
    void confirmUsesPessimisticFlockLock() {
        ChickenSale sale = draftSale(10);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));
        service.confirm(sale.getId());
        verify(flockRepository).findByIdForUpdate(10L);
        verify(flockRepository, never()).findById(10L);
    }

    @Test
    void confirmFailureDoesNotLeaveConfirmedSaleWhenLockMissing() {
        ChickenSale sale = draftSale(10);
        when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
        when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> service.confirm(sale.getId()));
        assertEquals(ChickenSaleStatus.DRAFT, sale.getStatus());
        assertEquals(100, flock.getCurrentBirdCount());
    }

    private void stubLookups() {
        lenient().when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        lenient().when(flockRepository.findById(10L)).thenReturn(Optional.of(flock));
        lenient().when(clientRepository.findById(5L)).thenReturn(Optional.of(customer));
    }

    private ChickenSaleRequest validRequest(int qty, String unitPrice) {
        ChickenSaleItemRequest item = new ChickenSaleItemRequest();
        item.setPricingMethod(PricingMethod.PER_BIRD);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(unitPrice));
        ChickenSaleRequest req = new ChickenSaleRequest();
        req.setFarmId(1L);
        req.setFlockId(10L);
        req.setCustomerId(5L);
        req.setSaleDate(LocalDate.now());
        req.setSaleType(ChickenSaleType.RETAIL);
        req.setItems(List.of(item));
        return req;
    }

    private ChickenSale draftSale(int qty) {
        ChickenSale sale = new ChickenSale();
        setId(sale, ids.getAndIncrement());
        sale.setSaleNumber("CS-TEST");
        sale.setStatus(ChickenSaleStatus.DRAFT);
        sale.setFarm(farm);
        sale.setFlock(flock);
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDate.now());
        sale.setSaleType(ChickenSaleType.RETAIL);
        var item = new com.trustagro.farm.sale.entity.ChickenSaleItem();
        item.setFlock(flock);
        item.setPricingMethod(PricingMethod.PER_BIRD);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal("750"));
        item.setLineSubtotal(new BigDecimal("750").multiply(BigDecimal.valueOf(qty)));
        sale.addItem(item);
        sale.setSubtotal(item.getLineSubtotal());
        sale.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        sale.setTaxAmount(BigDecimal.ZERO.setScale(2));
        sale.setTotalAmount(item.getLineSubtotal().setScale(2));
        sale.setPaidAmount(BigDecimal.ZERO.setScale(2));
        sale.setOutstandingAmount(sale.getTotalAmount());
        return sale;
    }

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
