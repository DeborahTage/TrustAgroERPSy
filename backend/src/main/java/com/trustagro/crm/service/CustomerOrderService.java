package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.dto.*;
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
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    public static final String SOURCE_MODULE = FinanceSourceTypes.MODULE_CRM;
    private static final String MODULE = "CRM";
    private static final String SEQ_ORDER = "ORDER";
    private static final String SEQ_PAYMENT = "ORDER_PAYMENT";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CustomerOrderRepository orderRepository;
    private final CrmClientRepository clientRepository;
    private final CrmQuotationRepository quotationRepository;
    private final CrmOpportunityRepository opportunityRepository;
    private final CrmOrderPaymentRepository paymentRepository;
    private final CrmNumberSequenceRepository sequenceRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinanceService financeService;
    private final WithdrawalService withdrawalService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return toResponse(requireOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByClient(Long clientId) {
        return orderRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream().map(this::toResponse).toList();
    }

    /** Backward-compatible entity create used by legacy callers. */
    @Transactional
    public CustomerOrder createOrder(CustomerOrder order) {
        if (order.getClient() == null || order.getClient().getId() == null) {
            throw new BusinessException("Client is required");
        }
        CrmClient client = clientRepository.findById(order.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + order.getClient().getId()));
        order.setClient(client);
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(nextOrderNumber());
        }
        if (order.getOrderDate() == null) order.setOrderDate(LocalDate.now());
        if (order.getStatus() == null) order.setStatus(CustomerOrderStatus.DRAFT);
        if (order.getCurrency() == null) order.setCurrency("ETB");
        order.setCreatedBy(currentUser());
        recalculateTotals(order);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("CREATE", MODULE, "CustomerOrder", saved.getId(), null, saved.getOrderNumber());
        return saved;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest req) {
        CrmClient client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.getClientId()));
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(nextOrderNumber());
        order.setClient(client);
        order.setOrderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now());
        order.setExpectedDeliveryDate(req.getExpectedDeliveryDate());
        order.setCurrency(req.getCurrency() != null ? req.getCurrency() : "ETB");
        order.setDiscountAmount(nvl(req.getDiscountAmount()));
        order.setTaxAmount(nvl(req.getTaxAmount()));
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setContactNumber(req.getContactNumber());
        order.setNotes(req.getNotes());
        order.setStatus(CustomerOrderStatus.DRAFT);
        order.setCreatedBy(currentUser());
        if (req.getQuotationId() != null) {
            quotationRepository.findById(req.getQuotationId()).ifPresent(order::setQuotation);
        }
        if (req.getOpportunityId() != null) {
            opportunityRepository.findById(req.getOpportunityId()).ifPresent(order::setOpportunity);
        }
        applyItems(order, req.getItems());
        recalculateTotals(order);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("CREATE", MODULE, "CustomerOrder", saved.getId(), null, saved.getOrderNumber());
        notificationService.createRoleNotification(
                "Customer order created",
                saved.getOrderNumber() + " for " + client.getClientName(),
                Role.CRM_MANAGER);
        return toResponse(saved);
    }

    /**
     * Idempotent order creation from an accepted quotation.
     * If an order already exists for the quotation, returns it.
     */
    @Transactional
    public CustomerOrder createFromQuotation(CrmQuotation quotation) {
        Optional<CustomerOrder> existing = orderRepository.findByQuotationId(quotation.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(nextOrderNumber());
        order.setClient(quotation.getClient());
        order.setQuotation(quotation);
        order.setOpportunity(quotation.getOpportunity());
        order.setOrderDate(LocalDate.now());
        order.setCurrency(quotation.getCurrency() != null ? quotation.getCurrency() : "ETB");
        order.setDiscountAmount(nvl(quotation.getDiscountAmount()));
        order.setTaxAmount(nvl(quotation.getTaxAmount()));
        order.setNotes(quotation.getNotes());
        order.setStatus(CustomerOrderStatus.DRAFT);
        order.setCreatedBy(currentUser());

        List<OrderItem> items = new ArrayList<>();
        for (CrmQuotationItem qi : quotation.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductName(qi.getProductOrService() != null ? qi.getProductOrService() : qi.getDescription());
            item.setQuantity(qi.getQuantity());
            item.setUnit(qi.getUnit());
            item.setUnitPrice(qi.getUnitPrice());
            item.setSubtotal(qi.getLineTotal());
            items.add(item);
        }
        order.setItems(items);
        order.setSubtotal(nvl(quotation.getSubtotal()));
        order.setTotalAmount(nvl(quotation.getTotalAmount()));
        try {
            CustomerOrder saved = orderRepository.save(order);
            auditService.log("CREATE_FROM_QUOTATION", MODULE, "CustomerOrder",
                    saved.getId(), quotation.getQuotationNumber(), saved.getOrderNumber());
            return saved;
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            return orderRepository.findByQuotationId(quotation.getId())
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public OrderResponse confirm(Long id) {
        CustomerOrder order = requireOrder(id);
        CustomerOrderLifecycle.assertTransition(order.getStatus(), CustomerOrderStatus.CONFIRMED);
        order.setStatus(CustomerOrderStatus.CONFIRMED);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("CONFIRM", MODULE, "CustomerOrder", saved.getId(),
                CustomerOrderStatus.DRAFT.name(), CustomerOrderStatus.CONFIRMED.name());
        notificationService.createRoleNotification(
                "Customer order confirmed",
                saved.getOrderNumber() + " confirmed",
                Role.CRM_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        CustomerOrder order = requireOrder(id);
        if (order.getInvoice() != null && nvl(order.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot cancel an order with recorded payments");
        }
        CustomerOrderLifecycle.assertTransition(order.getStatus(), CustomerOrderStatus.CANCELLED);
        CustomerOrderStatus prev = order.getStatus();
        order.setStatus(CustomerOrderStatus.CANCELLED);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("CANCEL", MODULE, "CustomerOrder", saved.getId(), prev.name(), CustomerOrderStatus.CANCELLED.name());
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse fulfill(Long id, boolean partial) {
        CustomerOrder order = requireOrder(id);
        CustomerOrderStatus next = partial
                ? CustomerOrderStatus.PARTIALLY_FULFILLED
                : CustomerOrderStatus.FULFILLED;
        if (order.getStatus() == CustomerOrderStatus.CONFIRMED && !partial) {
            // allow CONFIRMED → FULFILLED
        } else if (order.getStatus() == CustomerOrderStatus.CONFIRMED && partial) {
            CustomerOrderLifecycle.assertTransition(order.getStatus(), CustomerOrderStatus.IN_PROGRESS);
            order.setStatus(CustomerOrderStatus.IN_PROGRESS);
            orderRepository.save(order);
        }
        CustomerOrderLifecycle.assertTransition(order.getStatus(), next);
        CustomerOrderStatus prev = order.getStatus();
        order.setStatus(next);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("FULFILL", MODULE, "CustomerOrder", saved.getId(), prev.name(), next.name());
        return toResponse(saved);
    }

    /** Legacy raw status update — maps through lifecycle when possible. */
    @Transactional
    public CustomerOrder updateStatus(Long id, String status) {
        CustomerOrder order = requireOrder(id);
        CustomerOrderStatusConverter converter = new CustomerOrderStatusConverter();
        CustomerOrderStatus next = converter.convertToEntityAttribute(status);
        if (order.getStatus() != next) {
            CustomerOrderLifecycle.assertTransition(order.getStatus(), next);
            order.setStatus(next);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public OrderResponse createInvoice(Long id) {
        CustomerOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() == CustomerOrderStatus.DRAFT || order.getStatus() == CustomerOrderStatus.CANCELLED) {
            throw new BusinessException("Order must be confirmed (or in progress) before invoicing");
        }
        if (order.getInvoice() != null) {
            return toResponse(order);
        }
        var existing = invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                SOURCE_MODULE, FinanceSourceTypes.REF_CRM_ORDER, order.getId());
        if (existing.isPresent()) {
            order.setInvoice(existing.get());
            orderRepository.save(order);
            return toResponse(order);
        }

        BigDecimal total = nvl(order.getTotalAmount());
        BigDecimal subtotal = nvl(order.getSubtotal());
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            subtotal = total.subtract(nvl(order.getTaxAmount())).max(ZERO);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.CUSTOMER_ORDER);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(order.getClient().getId());
        invoice.setClientName(order.getClient().getClientName());
        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(nvl(order.getDiscountAmount()));
        invoice.setVatAmount(nvl(order.getTaxAmount()));
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(ZERO);
        invoice.setBusinessUnit(BusinessUnit.GENERAL);
        invoice.setSourceModule(SOURCE_MODULE);
        invoice.setSourceType(FinanceSourceTypes.REF_CRM_ORDER);
        invoice.setSourceId(order.getId());
        invoice.setDueDate(LocalDate.now().plusDays(14));

        if (order.getItems() != null) {
            for (OrderItem line : order.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setDescription(line.getProductName());
                item.setQuantity(line.getQuantity() != null ? line.getQuantity() : 1.0);
                item.setUnitPrice(nvl(line.getUnitPrice()));
                item.setLineTotal(nvl(line.getSubtotal()));
                item.setAccountCode(AccountCode.REV_OTHER);
                invoice.addItem(item);
            }
        }
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            InvoiceItem item = new InvoiceItem();
            item.setDescription("Customer order " + order.getOrderNumber());
            item.setQuantity(1.0);
            item.setUnitPrice(total);
            item.setLineTotal(total);
            item.setAccountCode(AccountCode.REV_OTHER);
            invoice.addItem(item);
        }

        Invoice savedInvoice;
        try {
            savedInvoice = invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            Invoice raced = invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                            SOURCE_MODULE, FinanceSourceTypes.REF_CRM_ORDER, order.getId())
                    .orElseThrow(() -> ex);
            order.setInvoice(raced);
            orderRepository.save(order);
            return toResponse(order);
        }

        // Never double-post revenue: only post after a newly persisted invoice
        BigDecimal netRevenue = subtotal.subtract(nvl(order.getDiscountAmount())).max(ZERO);
        if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.INCOME, netRevenue, BusinessUnit.GENERAL,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.REV_OTHER.getCode(),
                    "CRM order invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_REVENUE, savedInvoice.getId());
        }
        if (nvl(order.getTaxAmount()).compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.TRANSFER, order.getTaxAmount(), BusinessUnit.GENERAL,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.LIABILITY_VAT.getCode(),
                    "VAT on CRM order invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_VAT, savedInvoice.getId());
        }

        order.setInvoice(savedInvoice);
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("INVOICE", MODULE, "CustomerOrder", saved.getId(),
                null, savedInvoice.getInvoiceNumber());
        notificationService.createRoleNotification(
                "Customer order invoiced",
                saved.getOrderNumber() + " invoiced as " + savedInvoice.getInvoiceNumber(),
                Role.FINANCE);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse recordPayment(Long id, OrderPaymentRequest req) {
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<CrmOrderPayment> byKey = paymentRepository.findByIdempotencyKey(req.getIdempotencyKey().trim());
            if (byKey.isPresent()) {
                return toResponse(byKey.get().getOrder());
            }
        }

        CustomerOrder order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getInvoice() == null) {
            throw new BusinessException("Cannot record payment before the order is invoiced");
        }
        if (req.getPaymentMethod() == PaymentMethod.CREDIT) {
            throw new BusinessException("CREDIT is not a receipt method. Use CASH, BANK_TRANSFER, or MOBILE_MONEY");
        }
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        BigDecimal outstanding = nvl(order.getTotalAmount()).subtract(nvl(order.getPaidAmount())).max(ZERO);
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException("Payment exceeds outstanding amount of " + outstanding);
        }

        CrmOrderPayment payment = new CrmOrderPayment();
        payment.setPaymentNumber(nextPaymentNumber());
        payment.setOrder(order);
        payment.setInvoice(order.getInvoice());
        payment.setAmount(amount);
        payment.setMethod(req.getPaymentMethod());
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setRecordedBy(currentUser());
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            payment.setIdempotencyKey(req.getIdempotencyKey().trim());
        }
        try {
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
                return toResponse(paymentRepository.findByIdempotencyKey(req.getIdempotencyKey().trim())
                        .orElseThrow(() -> ex)
                        .getOrder());
            }
            throw ex;
        }

        String debit = req.getPaymentMethod() == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();
        var tx = financeService.postTransaction(
                TransactionType.TRANSFER, amount, BusinessUnit.GENERAL,
                debit, AccountCode.ASSET_RECEIVABLE.getCode(),
                "CRM order payment " + payment.getPaymentNumber(),
                SOURCE_MODULE, FinanceSourceTypes.REF_PAYMENT, payment.getId(),
                req.getPaymentMethod(), payment.getPaymentDate());
        payment.setFinanceTransactionId(tx.getId());
        paymentRepository.save(payment);

        Invoice invoice = order.getInvoice();
        invoice.setPaidAmount(nvl(invoice.getPaidAmount()).add(amount));
        BigDecimal invOutstanding = nvl(invoice.getTotalAmount()).subtract(nvl(invoice.getPaidAmount())).max(ZERO);
        invoice.setStatus(invOutstanding.compareTo(BigDecimal.ZERO) == 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        order.setPaidAmount(nvl(order.getPaidAmount()).add(amount));
        CustomerOrder saved = orderRepository.save(order);
        auditService.log("PAYMENT", MODULE, "CustomerOrder", saved.getId(),
                amount.toPlainString(), payment.getPaymentNumber());
        notificationService.createRoleNotification(
                "Customer order payment received",
                saved.getOrderNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.FINANCE);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderPaymentResponse> listPayments(Long orderId) {
        requireOrder(orderId);
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toPaymentResponse).toList();
    }

    private void applyItems(CustomerOrder order, List<OrderItemRequest> itemReqs) {
        List<OrderItem> items = new ArrayList<>();
        if (itemReqs != null) {
            for (OrderItemRequest ir : itemReqs) {
                if (ir.getFlockId() != null && "MEAT".equalsIgnoreCase(ir.getCategory())) {
                    withdrawalService.validateSalesEligibility(ir.getFlockId());
                }
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProductName(ir.getProductName());
                item.setCategory(ir.getCategory());
                item.setFlockId(ir.getFlockId());
                item.setQuantity(ir.getQuantity());
                item.setUnit(ir.getUnit());
                item.setUnitPrice(nvl(ir.getUnitPrice()));
                BigDecimal qty = BigDecimal.valueOf(ir.getQuantity() != null ? ir.getQuantity() : 0);
                item.setSubtotal(item.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP));
                items.add(item);
            }
        }
        order.setItems(items);
    }

    private void recalculateTotals(CustomerOrder order) {
        BigDecimal subtotal = ZERO;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getFlockId() != null && "MEAT".equalsIgnoreCase(item.getCategory())) {
                    withdrawalService.validateSalesEligibility(item.getFlockId());
                }
                item.setOrder(order);
                if (item.getSubtotal() == null && item.getUnitPrice() != null && item.getQuantity() != null) {
                    item.setSubtotal(item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP));
                }
                subtotal = subtotal.add(nvl(item.getSubtotal()));
            }
        }
        order.setSubtotal(subtotal);
        BigDecimal discount = nvl(order.getDiscountAmount());
        BigDecimal tax = nvl(order.getTaxAmount());
        order.setTotalAmount(subtotal.subtract(discount).add(tax).max(ZERO));
    }

    private CustomerOrder requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private String nextOrderNumber() {
        return nextNumber(SEQ_ORDER, "ORD");
    }

    private String nextPaymentNumber() {
        return nextNumber(SEQ_PAYMENT, "OPAY");
    }

    private String nextNumber(String sequenceName, String prefix) {
        CrmNumberSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> {
                    CrmNumberSequence fresh = new CrmNumberSequence();
                    fresh.setName(sequenceName);
                    fresh.setNextValue(1L);
                    return sequenceRepository.saveAndFlush(fresh);
                });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        sequenceRepository.save(seq);
        return String.format("%s-%06d", prefix, value);
    }

    private String nextInvoiceNumber() {
        return "CRMINV-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    OrderResponse toResponse(CustomerOrder o) {
        BigDecimal paid = nvl(o.getPaidAmount());
        BigDecimal total = nvl(o.getTotalAmount());
        List<OrderItemResponse> items = o.getItems() == null ? List.of() : o.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .id(i.getId())
                        .productName(i.getProductName())
                        .category(i.getCategory())
                        .flockId(i.getFlockId())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .clientId(o.getClient() != null ? o.getClient().getId() : null)
                .clientName(o.getClient() != null ? o.getClient().getClientName() : null)
                .quotationId(o.getQuotation() != null ? o.getQuotation().getId() : null)
                .opportunityId(o.getOpportunity() != null ? o.getOpportunity().getId() : null)
                .orderDate(o.getOrderDate())
                .expectedDeliveryDate(o.getExpectedDeliveryDate())
                .subtotal(o.getSubtotal())
                .discountAmount(o.getDiscountAmount())
                .taxAmount(o.getTaxAmount())
                .totalAmount(total)
                .paidAmount(paid)
                .outstandingAmount(total.subtract(paid).max(ZERO))
                .currency(o.getCurrency())
                .status(o.getStatus())
                .deliveryAddress(o.getDeliveryAddress())
                .contactNumber(o.getContactNumber())
                .notes(o.getNotes())
                .invoiceId(o.getInvoice() != null ? o.getInvoice().getId() : null)
                .invoiceNumber(o.getInvoice() != null ? o.getInvoice().getInvoiceNumber() : null)
                .createdById(o.getCreatedBy() != null ? o.getCreatedBy().getId() : null)
                .items(items)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private OrderPaymentResponse toPaymentResponse(CrmOrderPayment p) {
        return OrderPaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .orderId(p.getOrder().getId())
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .amount(p.getAmount())
                .method(p.getMethod())
                .paymentDate(p.getPaymentDate())
                .recordedById(p.getRecordedBy() != null ? p.getRecordedBy().getId() : null)
                .financeTransactionId(p.getFinanceTransactionId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
