package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.dto.OrderPaymentRequest;
import com.trustagro.crm.dto.OrderRequest;
import com.trustagro.crm.dto.OrderItemRequest;
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
import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.service.WithdrawalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.doThrow;

/**
 * Invoice / payment / idempotency authority for {@link CustomerOrderService}.
 */
@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    private static final String AR = AccountCode.ASSET_RECEIVABLE.getCode();
    private static final String REV = AccountCode.REV_OTHER.getCode();
    private static final String CASH = AccountCode.ASSET_CASH.getCode();
    private static final String BANK = AccountCode.ASSET_BANK.getCode();

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

    private User user;
    private CrmClient client;

    @BeforeEach
    void setUp() {
        setAuth(Permissions.CRM_ORDER, Permissions.CRM_ORDER_CONFIRM,
                Permissions.CRM_ORDER_INVOICE, Permissions.CRM_ORDER_PAY, Permissions.CRM_READ);

        user = new User();
        setId(user, 1L);
        user.setEmail("crm@test.com");
        user.setFullName("CRM Manager");
        lenient().when(userRepository.findByEmail("crm@test.com")).thenReturn(Optional.of(user));

        client = new CrmClient();
        setId(client, 5L);
        client.setClientName("Farm Client");
        client.setCustomerCode("CUS-000001");
        client.setStatus(ClientStatus.ACTIVE);
        lenient().when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

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
    void confirmThenInvoicePostsIncomeOnce_secondCallIsIdempotent() {
        CustomerOrder order = confirmedOrder(10L, "1000.00", "1000.00", "0.00");
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        OrderResponse first = service.createInvoice(10L);
        assertNotNull(first.getInvoiceId());
        assertEquals(order.getInvoice().getInvoiceNumber(), first.getInvoiceNumber());

        ArgumentCaptor<TransactionType> typeCap = ArgumentCaptor.forClass(TransactionType.class);
        verify(financeService).postTransaction(typeCap.capture(), eq(new BigDecimal("1000.00")),
                eq(BusinessUnit.GENERAL), eq(AR), eq(REV), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_INVOICE_REVENUE), any());
        assertEquals(TransactionType.INCOME, typeCap.getValue());
        verify(auditService).log(eq("INVOICE"), eq("CRM"), eq("CustomerOrder"), eq(10L), isNull(), anyString());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));

        // Second call — already linked; must not post again
        OrderResponse second = service.createInvoice(10L);
        assertEquals(first.getInvoiceId(), second.getInvoiceId());
        assertEquals(first.getInvoiceNumber(), second.getInvoiceNumber());
        verify(financeService, times(1)).postTransaction(eq(TransactionType.INCOME), any(), any(), any(), any(),
                any(), any(), any(), any());
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }

    @Test
    void secondInvoiceCallRelinksExistingSourceInvoiceWithoutRepost() {
        CustomerOrder order = confirmedOrder(11L, "500.00", "500.00", "0.00");
        Invoice existing = new Invoice();
        setId(existing, 99L);
        existing.setInvoiceNumber("CRMINV-EXISTING");
        existing.setTotalAmount(new BigDecimal("500.00"));
        existing.setPaidAmount(ZERO());
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_CRM_ORDER), eq(11L)))
                .thenReturn(Optional.of(existing));

        OrderResponse resp = service.createInvoice(11L);
        assertEquals(99L, resp.getInvoiceId());
        assertEquals("CRMINV-EXISTING", resp.getInvoiceNumber());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void draftOrderCannotBeInvoiced() {
        CustomerOrder order = confirmedOrder(12L, "100.00", "100.00", "0.00");
        order.setStatus(CustomerOrderStatus.DRAFT);
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        assertThrows(BusinessException.class, () -> service.createInvoice(12L));
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void paymentReducesOutstanding_partialThenFull_doesNotPostIncome() {
        CustomerOrder order = confirmedOrder(20L, "1000.00", "1000.00", "0.00");
        Invoice inv = new Invoice();
        setId(inv, 50L);
        inv.setInvoiceNumber("CRMINV-1");
        inv.setTotalAmount(new BigDecimal("1000.00"));
        inv.setPaidAmount(ZERO());
        inv.setStatus(InvoiceStatus.SENT);
        order.setInvoice(inv);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));

        OrderPaymentRequest partial = new OrderPaymentRequest();
        partial.setAmount(new BigDecimal("600.00"));
        partial.setPaymentMethod(PaymentMethod.CASH);
        OrderResponse afterPartial = service.recordPayment(20L, partial);
        assertEquals(0, new BigDecimal("600.00").compareTo(afterPartial.getPaidAmount()));
        assertEquals(0, new BigDecimal("400.00").compareTo(afterPartial.getOutstandingAmount()));
        assertEquals(InvoiceStatus.PARTIALLY_PAID, inv.getStatus());

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("600.00")),
                eq(BusinessUnit.GENERAL), eq(CASH), eq(AR), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_PAYMENT), any(),
                eq(PaymentMethod.CASH), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("600.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("600.00")), any(), any(), any(), any(), any(), any(), any());

        OrderPaymentRequest remainder = new OrderPaymentRequest();
        remainder.setAmount(new BigDecimal("400.00"));
        remainder.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        OrderResponse paid = service.recordPayment(20L, remainder);
        assertEquals(0, new BigDecimal("1000.00").compareTo(paid.getPaidAmount()));
        assertEquals(0, paid.getOutstandingAmount().compareTo(ZERO()));
        assertEquals(InvoiceStatus.PAID, inv.getStatus());

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("400.00")),
                eq(BusinessUnit.GENERAL), eq(BANK), eq(AR), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_PAYMENT), any(),
                eq(PaymentMethod.BANK_TRANSFER), any());
        // Still no INCOME from either payment
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                any(), any(), any(), any(), contains("payment"), any(), any(), any(), any(), any());
    }

    @Test
    void paymentBeforeInvoiceRejected() {
        CustomerOrder order = confirmedOrder(21L, "100.00", "100.00", "0.00");
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        OrderPaymentRequest req = new OrderPaymentRequest();
        req.setAmount(new BigDecimal("50.00"));
        req.setPaymentMethod(PaymentMethod.CASH);
        assertThrows(BusinessException.class, () -> service.recordPayment(21L, req));
    }

    @Test
    void paymentExceedingOutstandingRejected() {
        CustomerOrder order = confirmedOrder(22L, "100.00", "100.00", "0.00");
        Invoice inv = new Invoice();
        setId(inv, 51L);
        inv.setTotalAmount(new BigDecimal("100.00"));
        inv.setPaidAmount(ZERO());
        order.setInvoice(inv);
        when(orderRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(order));

        OrderPaymentRequest req = new OrderPaymentRequest();
        req.setAmount(new BigDecimal("150.00"));
        req.setPaymentMethod(PaymentMethod.CASH);
        assertThrows(BusinessException.class, () -> service.recordPayment(22L, req));
    }

    @Test
    void creditIsNotAReceiptMethod() {
        CustomerOrder order = confirmedOrder(23L, "100.00", "100.00", "0.00");
        Invoice inv = new Invoice();
        setId(inv, 52L);
        inv.setTotalAmount(new BigDecimal("100.00"));
        inv.setPaidAmount(ZERO());
        order.setInvoice(inv);
        when(orderRepository.findByIdForUpdate(23L)).thenReturn(Optional.of(order));

        OrderPaymentRequest req = new OrderPaymentRequest();
        req.setAmount(new BigDecimal("50.00"));
        req.setPaymentMethod(PaymentMethod.CREDIT);
        assertThrows(BusinessException.class, () -> service.recordPayment(23L, req));
    }

    @Test
    void unauthorizedRolesMissingCrmOrderPayAreDeniedByCatalog() {
        assertFalse(RolePermissionCatalog.hasPermission(Role.EXTENSION_WORKER, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.GENERAL_MANAGER, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.PHARMACIST, Permissions.CRM_ORDER_PAY));
        assertFalse(RolePermissionCatalog.hasPermission(Role.ADMIN, Permissions.CRM_ORDER_PAY));
        assertTrue(RolePermissionCatalog.hasPermission(Role.CRM_MANAGER, Permissions.CRM_ORDER_PAY));
        assertTrue(RolePermissionCatalog.hasPermission(Role.FINANCE, Permissions.CRM_ORDER_PAY));
    }

    @Test
    void createFromQuotationIsIdempotent() {
        CrmQuotation q = new CrmQuotation();
        setId(q, 30L);
        q.setQuotationNumber("QUO-000001");
        q.setClient(client);
        q.setCurrency("ETB");
        q.setSubtotal(new BigDecimal("200.00"));
        q.setDiscountAmount(ZERO());
        q.setTaxAmount(ZERO());
        q.setTotalAmount(new BigDecimal("200.00"));
        q.setStatus(QuotationStatus.ACCEPTED);
        CrmQuotationItem qi = new CrmQuotationItem();
        qi.setDescription("Feed");
        qi.setProductOrService("Feed");
        qi.setQuantity(2.0);
        qi.setUnitPrice(new BigDecimal("100.00"));
        qi.setLineTotal(new BigDecimal("200.00"));
        q.setItems(List.of(qi));

        when(orderRepository.findByQuotationId(30L)).thenReturn(Optional.empty());
        CustomerOrder first = service.createFromQuotation(q);
        assertEquals(CustomerOrderStatus.DRAFT, first.getStatus());
        assertEquals(0, new BigDecimal("200.00").compareTo(first.getTotalAmount()));
        verify(orderRepository, times(1)).save(any());

        when(orderRepository.findByQuotationId(30L)).thenReturn(Optional.of(first));
        CustomerOrder again = service.createFromQuotation(q);
        assertSame(first, again);
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void createOrderComputesTotalsFromItems() {
        OrderRequest req = new OrderRequest();
        req.setClientId(5L);
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Eggs");
        item.setQuantity(10.0);
        item.setUnitPrice(new BigDecimal("15.00"));
        req.setItems(List.of(item));
        req.setDiscountAmount(new BigDecimal("10.00"));
        req.setTaxAmount(new BigDecimal("5.00"));

        OrderResponse created = service.createOrder(req);
        assertEquals(CustomerOrderStatus.DRAFT, created.getStatus());
        assertEquals(0, new BigDecimal("150.00").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("145.00").compareTo(created.getTotalAmount()));
        verify(auditService).log(eq("CREATE"), eq("CRM"), eq("CustomerOrder"), any(), isNull(), anyString());
    }

    @Test
    void invoiceWithVatPostsTransferForVat() {
        CustomerOrder order = confirmedOrder(40L, "1100.00", "1000.00", "100.00");
        when(orderRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(order));

        service.createInvoice(40L);

        verify(financeService).postTransaction(eq(TransactionType.INCOME), eq(new BigDecimal("1000.00")),
                eq(BusinessUnit.GENERAL), eq(AR), eq(REV), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_INVOICE_REVENUE), any());
        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("100.00")),
                eq(BusinessUnit.GENERAL), eq(AR), eq(AccountCode.LIABILITY_VAT.getCode()), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_INVOICE_VAT), any());
    }

    @Test
    void createFromQuotationRaceRefetchesOnUniqueViolation() {
        CrmQuotation q = new CrmQuotation();
        setId(q, 31L);
        q.setQuotationNumber("QUO-000031");
        q.setClient(client);
        q.setCurrency("ETB");
        q.setSubtotal(new BigDecimal("100.00"));
        q.setDiscountAmount(ZERO());
        q.setTaxAmount(ZERO());
        q.setTotalAmount(new BigDecimal("100.00"));
        q.setItems(List.of());

        CustomerOrder raced = confirmedOrder(310L, "100.00", "100.00", "0.00");
        raced.setStatus(CustomerOrderStatus.DRAFT);
        raced.setQuotation(q);

        when(orderRepository.findByQuotationId(31L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(raced));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("uk_customer_order_quotation"))
                .when(orderRepository).save(any(CustomerOrder.class));

        CustomerOrder result = service.createFromQuotation(q);
        assertSame(raced, result);
        verify(orderRepository, times(2)).findByQuotationId(31L);
    }

    @Test
    void paymentIdempotencyKeyReturnsExistingWithoutDoublePost() {
        CustomerOrder order = confirmedOrder(24L, "100.00", "100.00", "0.00");
        Invoice inv = new Invoice();
        setId(inv, 53L);
        inv.setTotalAmount(new BigDecimal("100.00"));
        inv.setPaidAmount(new BigDecimal("40.00"));
        inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        order.setInvoice(inv);
        order.setPaidAmount(new BigDecimal("40.00"));

        CrmOrderPayment existing = new CrmOrderPayment();
        setId(existing, 900L);
        existing.setPaymentNumber("OPAY-1");
        existing.setOrder(order);
        existing.setAmount(new BigDecimal("40.00"));
        existing.setIdempotencyKey("pay-key-1");

        when(paymentRepository.findByIdempotencyKey("pay-key-1")).thenReturn(Optional.of(existing));

        OrderPaymentRequest req = new OrderPaymentRequest();
        req.setAmount(new BigDecimal("40.00"));
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setIdempotencyKey("pay-key-1");

        OrderResponse resp = service.recordPayment(24L, req);
        assertEquals(0, new BigDecimal("40.00").compareTo(resp.getPaidAmount()));
        verify(paymentRepository, never()).save(any());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private CustomerOrder confirmedOrder(Long id, String total, String subtotal, String tax) {
        CustomerOrder order = new CustomerOrder();
        setId(order, id);
        order.setOrderNumber("ORD-" + id);
        order.setClient(client);
        order.setStatus(CustomerOrderStatus.CONFIRMED);
        order.setCurrency("ETB");
        order.setSubtotal(new BigDecimal(subtotal));
        order.setDiscountAmount(ZERO());
        order.setTaxAmount(new BigDecimal(tax));
        order.setTotalAmount(new BigDecimal(total));
        order.setPaidAmount(ZERO());
        order.setOrderDate(LocalDate.now());
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductName("Product");
        item.setQuantity(1.0);
        item.setUnitPrice(new BigDecimal(subtotal));
        item.setSubtotal(new BigDecimal(subtotal));
        order.setItems(new ArrayList<>(List.of(item)));
        return order;
    }

    private void setAuth(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "crm@test.com", "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
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

    private BigDecimal ZERO() {
        return BigDecimal.ZERO.setScale(2);
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
