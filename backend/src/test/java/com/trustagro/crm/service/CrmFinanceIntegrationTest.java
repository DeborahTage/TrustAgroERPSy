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

/**
 * Focused finance assertions for CRM order invoice + AR payments.
 */
@ExtendWith(MockitoExtension.class)
class CrmFinanceIntegrationTest {

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
    void revenuePostedOnce_arProgresses10000To6000To0_paymentsAreTransfer() {
        CustomerOrder order = order(100L, "10000.00");
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));

        OrderResponse invoiced = service.createInvoice(100L);
        assertNotNull(invoiced.getInvoiceId());

        ArgumentCaptor<TransactionType> incomeType = ArgumentCaptor.forClass(TransactionType.class);
        verify(financeService).postTransaction(incomeType.capture(), eq(new BigDecimal("10000.00")),
                eq(BusinessUnit.GENERAL), eq(AR), eq(REV), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_INVOICE_REVENUE), any());
        assertEquals(TransactionType.INCOME, incomeType.getValue());

        // Second invoice call — no second INCOME
        service.createInvoice(100L);
        verify(financeService, times(1)).postTransaction(eq(TransactionType.INCOME), any(), any(), any(), any(),
                any(), any(), any(), any());

        assertEquals(0, new BigDecimal("10000.00").compareTo(
                order.getTotalAmount().subtract(order.getPaidAmount())));

        OrderPaymentRequest p1 = new OrderPaymentRequest();
        p1.setAmount(new BigDecimal("4000.00"));
        p1.setPaymentMethod(PaymentMethod.CASH);
        OrderResponse after1 = service.recordPayment(100L, p1);
        assertEquals(0, new BigDecimal("6000.00").compareTo(after1.getOutstandingAmount()));

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("4000.00")),
                eq(BusinessUnit.GENERAL), eq(CASH), eq(AR), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_PAYMENT), any(),
                eq(PaymentMethod.CASH), any());

        OrderPaymentRequest p2 = new OrderPaymentRequest();
        p2.setAmount(new BigDecimal("6000.00"));
        p2.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        OrderResponse after2 = service.recordPayment(100L, p2);
        assertEquals(0, after2.getOutstandingAmount().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(InvoiceStatus.PAID, order.getInvoice().getStatus());

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("6000.00")),
                eq(BusinessUnit.GENERAL), eq(BANK), eq(AR), anyString(),
                eq(FinanceSourceTypes.MODULE_CRM), eq(FinanceSourceTypes.REF_PAYMENT), any(),
                eq(PaymentMethod.BANK_TRANSFER), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("4000.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("6000.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private CustomerOrder order(Long id, String total) {
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
        item.setProductName("Product");
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
