package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.crm.dto.OrderPaymentRequest;
import com.trustagro.crm.dto.OrderResponse;
import com.trustagro.crm.entity.*;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.crm.repository.CrmNumberSequenceRepository;
import com.trustagro.crm.repository.CrmOpportunityRepository;
import com.trustagro.crm.repository.CrmOrderPaymentRepository;
import com.trustagro.crm.repository.CrmQuotationRepository;
import com.trustagro.crm.repository.CustomerOrderRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Concurrent createFromQuotation / createInvoice / payment idempotency races.
 */
@ExtendWith(MockitoExtension.class)
class CrmConcurrencyTest {

    @Mock CustomerOrderRepository orderRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock CrmQuotationRepository quotationRepository;
    @Mock CrmOpportunityRepository opportunityRepository;
    @Mock CrmOrderPaymentRepository paymentRepository;
    @Mock CrmNumberSequenceRepository sequenceRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock FinanceService financeService;
    @Mock WithdrawalService withdrawalService;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks CustomerOrderService service;

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, CrmNumberSequence> sequences = new HashMap<>();
    private CrmClient client;

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "crm@test.com", "x",
                List.of(new SimpleGrantedAuthority(Permissions.CRM_ORDER),
                        new SimpleGrantedAuthority(Permissions.CRM_ORDER_INVOICE),
                        new SimpleGrantedAuthority(Permissions.CRM_ORDER_PAY)));
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        setId(user, 1L);
        user.setEmail("crm@test.com");
        lenient().when(userRepository.findByEmail("crm@test.com")).thenReturn(Optional.of(user));

        client = new CrmClient();
        setId(client, 5L);
        client.setClientName("Farm Client");
        client.setStatus(ClientStatus.ACTIVE);

        lenient().when(sequenceRepository.findByNameForUpdate(anyString())).thenAnswer(inv ->
                Optional.of(sequences.computeIfAbsent(inv.getArgument(0), name -> {
                    CrmNumberSequence seq = new CrmNumberSequence();
                    seq.setName(name);
                    seq.setNextValue(1L);
                    return seq;
                })));
        lenient().when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sequenceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i == null) return null;
            if (i.getId() == null) setId(i, 50L);
            return i;
        });
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(77L));
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(78L));
        lenient().when(invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentCreateFromQuotation_uniqueViolationReturnsSingleOrder() {
        CrmQuotation q = new CrmQuotation();
        setId(q, 40L);
        q.setQuotationNumber("QUO-000040");
        q.setClient(client);
        q.setCurrency("ETB");
        q.setSubtotal(new BigDecimal("250.00"));
        q.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        q.setTaxAmount(BigDecimal.ZERO.setScale(2));
        q.setTotalAmount(new BigDecimal("250.00"));
        q.setItems(List.of());

        CustomerOrder winner = confirmed(400L, "250.00");
        winner.setStatus(CustomerOrderStatus.DRAFT);
        winner.setQuotation(q);

        when(orderRepository.findByQuotationId(40L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        doThrow(new DataIntegrityViolationException("uk_customer_order_quotation"))
                .when(orderRepository).save(any(CustomerOrder.class));

        CustomerOrder result = service.createFromQuotation(q);
        assertEquals(400L, result.getId());
        verify(orderRepository, times(1)).save(any(CustomerOrder.class));
    }

    @Test
    void concurrentCreateInvoice_uniqueViolationRefetchesWithoutSecondIncome() {
        CustomerOrder order = confirmed(50L, "500.00");
        when(orderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));

        Invoice raced = new Invoice();
        setId(raced, 88L);
        raced.setInvoiceNumber("CRMINV-RACE");
        raced.setTotalAmount(new BigDecimal("500.00"));
        raced.setPaidAmount(BigDecimal.ZERO.setScale(2));
        raced.setSourceModule(FinanceSourceTypes.MODULE_CRM);
        raced.setSourceType(FinanceSourceTypes.REF_CRM_ORDER);
        raced.setSourceId(50L);

        doThrow(new DataIntegrityViolationException("uk_invoice_source_ref"))
                .when(invoiceRepository).save(any(Invoice.class));
        when(invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_CRM_ORDER), eq(50L)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(raced));

        OrderResponse resp = service.createInvoice(50L);
        assertEquals(88L, resp.getInvoiceId());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void concurrentPaymentSameIdempotencyKey_returnsExisting() {
        CustomerOrder order = confirmed(60L, "100.00");
        Invoice inv = new Invoice();
        setId(inv, 61L);
        inv.setTotalAmount(new BigDecimal("100.00"));
        inv.setPaidAmount(new BigDecimal("30.00"));
        inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        order.setInvoice(inv);
        order.setPaidAmount(new BigDecimal("30.00"));

        CrmOrderPayment existing = new CrmOrderPayment();
        setId(existing, 70L);
        existing.setPaymentNumber("OPAY-70");
        existing.setOrder(order);
        existing.setAmount(new BigDecimal("30.00"));
        existing.setIdempotencyKey("idem-60");

        // First check empty (race), then save throws, then re-fetch finds existing
        when(paymentRepository.findByIdempotencyKey("idem-60"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(orderRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(order));
        doThrow(new DataIntegrityViolationException("uk_crm_order_payment_idempotency"))
                .when(paymentRepository).save(any(CrmOrderPayment.class));

        OrderPaymentRequest req = new OrderPaymentRequest();
        req.setAmount(new BigDecimal("30.00"));
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setIdempotencyKey("idem-60");

        OrderResponse resp = service.recordPayment(60L, req);
        assertEquals(0, new BigDecimal("30.00").compareTo(resp.getPaidAmount()));
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private CustomerOrder confirmed(Long id, String total) {
        CustomerOrder order = new CustomerOrder();
        setId(order, id);
        order.setOrderNumber("ORD-" + id);
        order.setClient(client);
        order.setStatus(CustomerOrderStatus.CONFIRMED);
        order.setCurrency("ETB");
        order.setSubtotal(new BigDecimal(total));
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        order.setTaxAmount(BigDecimal.ZERO.setScale(2));
        order.setTotalAmount(new BigDecimal(total));
        order.setPaidAmount(BigDecimal.ZERO.setScale(2));
        order.setOrderDate(LocalDate.now());
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductName("P");
        item.setQuantity(1.0);
        item.setUnitPrice(new BigDecimal(total));
        item.setSubtotal(new BigDecimal(total));
        order.setItems(new ArrayList<>(List.of(item)));
        return order;
    }

    private FinanceTransaction tx(Long id) {
        FinanceTransaction t = new FinanceTransaction();
        setId(t, id);
        return t;
    }

    private <T> T withId(T entity) {
        if (entity == null) {
            return null;
        }
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, ids.getAndIncrement());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entity;
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
