package com.trustagro.farm.sale.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.farm.entity.Barn;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.BarnRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.farm.sale.dto.*;
import com.trustagro.farm.sale.entity.*;
import com.trustagro.farm.sale.repository.ChickenSalePaymentRepository;
import com.trustagro.farm.sale.repository.ChickenSaleRepository;
import com.trustagro.farm.sale.repository.FlockPopulationEventRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.FlockCOGSService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChickenSaleService {

    public static final String SOURCE_MODULE = "CHICKEN_SALE";
    public static final String AUDIT_MODULE = "CHICKEN_SALE";

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ChickenSaleRepository saleRepository;
    private final ChickenSalePaymentRepository paymentRepository;
    private final FlockPopulationEventRepository populationEventRepository;
    private final FlockRepository flockRepository;
    private final FarmRepository farmRepository;
    private final BarnRepository barnRepository;
    private final CrmClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinanceService financeService;
    private final WithdrawalService withdrawalService;
    private final FlockCOGSService flockCOGSService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final FarmAccessService farmAccessService;

    @Transactional(readOnly = true)
    public List<ChickenSaleResponse> list(ChickenSaleStatus status, Long customerId, Long flockId, Long farmId) {
        List<Long> scope = farmAccessService.accessibleFarmIdsOrNull();
        if (farmId != null) {
            farmAccessService.assertCanAccessFarm(farmId);
        }
        List<ChickenSale> sales;
        if (customerId != null) {
            sales = saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId);
        } else if (flockId != null) {
            sales = saleRepository.findByFlockIdOrderBySaleDateDesc(flockId);
        } else if (farmId != null) {
            sales = saleRepository.findByFarmIdOrderBySaleDateDesc(farmId);
        } else if (status != null) {
            sales = saleRepository.findByStatusOrderBySaleDateDesc(status);
        } else {
            sales = saleRepository.findAll();
        }
        return sales.stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> customerId == null || s.getCustomer().getId().equals(customerId))
                .filter(s -> flockId == null || s.getFlock().getId().equals(flockId))
                .filter(s -> farmId == null || s.getFarm().getId().equals(farmId))
                .filter(s -> scope == null || scope.contains(s.getFarm().getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChickenSaleResponse getById(Long id) {
        ChickenSale sale = findSale(id);
        farmAccessService.assertCanAccessFarm(sale.getFarm().getId());
        return toResponse(sale);
    }

    @Transactional
    public ChickenSaleResponse create(ChickenSaleRequest req) {
        farmAccessService.assertCanAccessFarm(req.getFarmId());
        ChickenSale sale = new ChickenSale();
        sale.setSaleNumber(nextSaleNumber());
        sale.setStatus(ChickenSaleStatus.DRAFT);
        sale.setCreatedBy(currentUser());
        applyDraftFields(sale, req);
        ChickenSale saved = saleRepository.save(sale);
        auditService.log("CREATE", AUDIT_MODULE, "ChickenSale", saved.getId(), null, saved.getSaleNumber());
        return toResponse(saved);
    }

    @Transactional
    public ChickenSaleResponse update(Long id, ChickenSaleRequest req) {
        ChickenSale sale = findSale(id);
        if (!ChickenSaleLifecycle.isEditable(sale.getStatus())) {
            throw new BusinessException("Only DRAFT sales can be updated");
        }
        applyDraftFields(sale, req);
        ChickenSale saved = saleRepository.save(sale);
        auditService.log("UPDATE", AUDIT_MODULE, "ChickenSale", saved.getId(), ChickenSaleStatus.DRAFT.name(), saved.getSaleNumber());
        return toResponse(saved);
    }

    /**
     * Confirm is the flock-custody boundary: lock flock, check population, decrement,
     * write population history, persist sale — all in one transaction.
     */
    @Transactional
    public ChickenSaleResponse confirm(Long id) {
        ChickenSale sale = findSale(id);
        ChickenSaleLifecycle.assertTransition(sale.getStatus(), ChickenSaleStatus.CONFIRMED);

        Flock flock = flockRepository.findByIdForUpdate(sale.getFlock().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        assertFlockSellable(flock);
        withdrawalService.validateSalesEligibility(flock.getId());

        int qty = sale.totalChickens();
        int available = flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : 0;
        if (qty > available) {
            throw new BusinessException("Cannot sell " + qty + " Chickens; flock " + flock.getBatchCode()
                    + " has only " + available + " available");
        }

        int previous = available;
        int remaining = available - qty;
        flock.setCurrentBirdCount(remaining);
        flockRepository.save(flock);

        recordPopulationEvent(flock, FlockPopulationEventType.SALE_CONFIRM, previous, qty, remaining,
                sale.getId(), "Confirmed sale " + sale.getSaleNumber());

        sale.setStatus(ChickenSaleStatus.CONFIRMED);
        sale.setConfirmedBy(currentUser());
        ChickenSale saved = saleRepository.save(sale);

        auditService.log("CONFIRM", AUDIT_MODULE, "ChickenSale", saved.getId(),
                String.valueOf(previous), String.valueOf(remaining));
        notificationService.createRoleNotification(
                "Chicken sale confirmed",
                saved.getSaleNumber() + " — " + qty + " Chickens from " + flock.getBatchCode(),
                Role.FINANCE);
        notificationService.createRoleNotification(
                "Chicken sale confirmed",
                saved.getSaleNumber() + " — " + qty + " Chickens from " + flock.getBatchCode(),
                Role.FARM_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public ChickenSaleResponse invoice(Long id) {
        ChickenSale sale = findSale(id);
        ChickenSaleLifecycle.assertTransition(sale.getStatus(), ChickenSaleStatus.INVOICED);
        if (sale.getInvoice() != null) {
            throw new BusinessException("Invoice already exists for this sale");
        }
        if (invoiceRepository.findBySourceModuleAndSourceId(SOURCE_MODULE, sale.getId()).isPresent()) {
            throw new BusinessException("Invoice already exists for chicken sale " + sale.getSaleNumber());
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.CHICKEN_SALE);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(sale.getCustomer().getId());
        invoice.setClientName(sale.getCustomer().getClientName());
        invoice.setSubtotal(sale.getSubtotal());
        invoice.setDiscountAmount(sale.getDiscountAmount());
        invoice.setVatAmount(sale.getTaxAmount());
        invoice.setTotalAmount(sale.getTotalAmount());
        invoice.setPaidAmount(ZERO);
        invoice.setBusinessUnit(BusinessUnit.POULTRY_FARM);
        invoice.setSourceModule(SOURCE_MODULE);
        invoice.setSourceType(SOURCE_MODULE);
        invoice.setSourceId(sale.getId());
        invoice.setDueDate(LocalDate.now().plusDays(14));

        for (ChickenSaleItem line : sale.getItems()) {
            InvoiceItem item = new InvoiceItem();
            String desc = "Chicken sale " + sale.getSaleNumber() + " / " + sale.getFlock().getBatchCode()
                    + " (" + line.getPricingMethod() + ")";
            item.setDescription(desc);
            item.setQuantity(line.getQuantity().doubleValue());
            BigDecimal unit = line.getPricingMethod() == PricingMethod.BY_WEIGHT
                    ? nvl(line.getPricePerKg())
                    : nvl(line.getUnitPrice());
            item.setUnitPrice(unit);
            item.setLineTotal(line.getLineSubtotal());
            item.setAccountCode(AccountCode.REV_POULTRY_SALES);
            invoice.addItem(item);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        BigDecimal netRevenue = sale.getSubtotal().subtract(sale.getDiscountAmount()).max(ZERO);
        if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.INCOME, netRevenue, BusinessUnit.POULTRY_FARM,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.REV_POULTRY_SALES.getCode(),
                    "Chicken sale invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_REVENUE, savedInvoice.getId());
        }
        if (sale.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.TRANSFER, sale.getTaxAmount(), BusinessUnit.POULTRY_FARM,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.LIABILITY_VAT.getCode(),
                    "VAT on chicken sale invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_VAT, savedInvoice.getId());
        }

        sale.setInvoice(savedInvoice);
        sale.setStatus(ChickenSaleStatus.INVOICED);
        ChickenSale saved = saleRepository.save(sale);

        auditService.log("INVOICE", AUDIT_MODULE, "ChickenSale", saved.getId(),
                ChickenSaleStatus.CONFIRMED.name(), savedInvoice.getInvoiceNumber());
        notificationService.createRoleNotification(
                "Chicken sale invoiced",
                saved.getSaleNumber() + " invoiced as " + savedInvoice.getInvoiceNumber(),
                Role.CRM_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public ChickenSaleResponse recordPayment(Long id, ChickenSalePaymentRequest req) {
        ChickenSale sale = findSale(id);
        if (sale.getInvoice() == null) {
            throw new BusinessException("Cannot record payment before the sale is invoiced");
        }
        if (req.getPaymentMethod() == PaymentMethod.CREDIT) {
            throw new BusinessException("CREDIT is not a receipt method. Use CASH, BANK_TRANSFER, or MOBILE_MONEY");
        }

        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(sale.getOutstandingAmount()) > 0) {
            throw new BusinessException("Payment exceeds outstanding amount of " + sale.getOutstandingAmount());
        }

        BigDecimal newPaid = sale.getPaidAmount().add(amount);
        BigDecimal newOutstanding = sale.getTotalAmount().subtract(newPaid).max(ZERO);
        ChickenSaleStatus next = newOutstanding.compareTo(BigDecimal.ZERO) == 0
                ? ChickenSaleStatus.PAID
                : ChickenSaleStatus.PARTIALLY_PAID;
        ChickenSaleLifecycle.assertTransition(sale.getStatus(), next);

        String debit = req.getPaymentMethod() == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();

        Invoice invoice = sale.getInvoice();

        ChickenSalePayment payment = new ChickenSalePayment();
        payment.setSale(sale);
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(amount);
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setNotes(req.getNotes());
        payment.setRecordedBy(currentUser());
        payment = paymentRepository.save(payment);

        // Settlement only — never additional revenue. Idempotent per payment row.
        FinanceTransaction tx = financeService.postTransaction(
                TransactionType.TRANSFER, amount, BusinessUnit.POULTRY_FARM,
                debit, AccountCode.ASSET_RECEIVABLE.getCode(),
                "Chicken sale payment " + sale.getSaleNumber() + " (" + req.getPaymentMethod() + ")",
                SOURCE_MODULE, FinanceSourceTypes.REF_PAYMENT, payment.getId(),
                req.getPaymentMethod(), payment.getPaymentDate());
        payment.setFinanceTransactionId(tx.getId());
        paymentRepository.save(payment);

        invoice.setPaidAmount(nvl(invoice.getPaidAmount()).add(amount));
        invoice.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        sale.setPaidAmount(newPaid);
        sale.setOutstandingAmount(newOutstanding);
        sale.setStatus(next);
        ChickenSale saved = saleRepository.save(sale);

        auditService.log("PAYMENT", AUDIT_MODULE, "ChickenSale", saved.getId(),
                amount.toPlainString(), next.name());
        notificationService.createRoleNotification(
                "Chicken sale payment received",
                saved.getSaleNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.FARM_MANAGER);
        return toResponse(saved);
    }

    @Transactional
    public ChickenSaleResponse complete(Long id) {
        ChickenSale sale = findSale(id);
        ChickenSaleLifecycle.assertTransition(sale.getStatus(), ChickenSaleStatus.COMPLETED);
        sale.setStatus(ChickenSaleStatus.COMPLETED);
        ChickenSale saved = saleRepository.save(sale);
        auditService.log("COMPLETE", AUDIT_MODULE, "ChickenSale", saved.getId(),
                ChickenSaleStatus.PAID.name(), ChickenSaleStatus.COMPLETED.name());
        notificationService.createRoleNotification(
                "Chicken sale completed",
                saved.getSaleNumber() + " completed",
                Role.FINANCE);
        return toResponse(saved);
    }

    @Transactional
    public ChickenSaleResponse cancel(Long id) {
        ChickenSale sale = findSale(id);
        if (!ChickenSaleLifecycle.isCancellable(sale.getStatus())) {
            throw new BusinessException("Sale cannot be cancelled after invoicing");
        }
        ChickenSaleLifecycle.assertTransition(sale.getStatus(), ChickenSaleStatus.CANCELLED);

        if (sale.getStatus() == ChickenSaleStatus.CONFIRMED) {
            Flock flock = flockRepository.findByIdForUpdate(sale.getFlock().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
            int qty = sale.totalChickens();
            int previous = flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : 0;
            int restored = previous + qty;
            flock.setCurrentBirdCount(restored);
            flockRepository.save(flock);
            recordPopulationEvent(flock, FlockPopulationEventType.SALE_CANCEL_RESTORE, previous, qty, restored,
                    sale.getId(), "Cancelled sale " + sale.getSaleNumber());
        }

        ChickenSaleStatus previousStatus = sale.getStatus();
        sale.setStatus(ChickenSaleStatus.CANCELLED);
        ChickenSale saved = saleRepository.save(sale);
        auditService.log("CANCEL", AUDIT_MODULE, "ChickenSale", saved.getId(),
                previousStatus.name(), ChickenSaleStatus.CANCELLED.name());
        notificationService.createRoleNotification(
                "Chicken sale cancelled",
                saved.getSaleNumber() + " was cancelled",
                Role.FINANCE);
        return toResponse(saved);
    }

    private void applyDraftFields(ChickenSale sale, ChickenSaleRequest req) {
        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        if (farm.getStatus() != FarmStatus.ACTIVE) {
            throw new BusinessException("Farm is not active");
        }
        Flock flock = flockRepository.findById(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        assertFlockSellable(flock);
        if (!flock.getFarm().getId().equals(farm.getId())) {
            throw new BusinessException("Flock does not belong to the selected farm");
        }
        withdrawalService.validateSalesEligibility(flock.getId());

        Barn barn = resolveBarn(req.getBarnId(), flock, farm);
        CrmClient customer = clientRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getStatus() == ClientStatus.INACTIVE || customer.getStatus() == ClientStatus.LOST) {
            throw new BusinessException("Customer is inactive and cannot be billed");
        }

        sale.setFarm(farm);
        sale.setBarn(barn);
        sale.setFlock(flock);
        sale.setCustomer(customer);
        sale.setSaleDate(req.getSaleDate());
        sale.setSaleType(req.getSaleType());
        sale.setNotes(req.getNotes());
        sale.clearItems();

        BigDecimal subtotal = ZERO;
        int Chickens = 0;
        for (ChickenSaleItemRequest lineReq : req.getItems()) {
            ChickenSaleItem item = buildItem(lineReq, flock);
            sale.addItem(item);
            subtotal = subtotal.add(item.getLineSubtotal());
            Chickens += item.getQuantity();
        }
        if (Chickens <= 0) {
            throw new BusinessException("Sale quantity must be greater than zero");
        }
        int available = flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : 0;
        if (Chickens > available) {
            throw new BusinessException("Cannot sell " + Chickens + " Chickens; flock " + flock.getBatchCode()
                    + " has only " + available + " available");
        }

        BigDecimal discount = nvl(req.getDiscountAmount());
        BigDecimal tax = nvl(req.getTaxAmount());
        if (discount.compareTo(BigDecimal.ZERO) < 0 || tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Discount and tax cannot be negative");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessException("Discount cannot exceed subtotal");
        }
        BigDecimal total = subtotal.subtract(discount).add(tax).setScale(2, RoundingMode.HALF_UP);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discount);
        sale.setTaxAmount(tax);
        sale.setTotalAmount(total);
        sale.setPaidAmount(ZERO);
        sale.setOutstandingAmount(total);
    }

    private ChickenSaleItem buildItem(ChickenSaleItemRequest req, Flock headerFlock) {
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new BusinessException("Line quantity must be greater than zero");
        }
        Flock lineFlock = headerFlock;
        if (req.getFlockId() != null && !req.getFlockId().equals(headerFlock.getId())) {
            throw new BusinessException("All sale lines must belong to the header flock");
        }
        PricingMethod method = req.getPricingMethod() != null ? req.getPricingMethod() : PricingMethod.PER_BIRD;
        BigDecimal lineTotal;
        if (method == PricingMethod.PER_BIRD) {
            BigDecimal unit = nvl(req.getUnitPrice());
            if (unit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("PER_BIRD lines require a unit price greater than zero");
            }
            lineTotal = unit.multiply(BigDecimal.valueOf(req.getQuantity()));
        } else if (method == PricingMethod.BY_WEIGHT) {
            BigDecimal weight = nvl(req.getWeightKg());
            BigDecimal perKg = nvl(req.getPricePerKg());
            if (weight.compareTo(BigDecimal.ZERO) <= 0 || perKg.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("BY_WEIGHT lines require weight and price per kg greater than zero");
            }
            lineTotal = weight.multiply(perKg);
        } else {
            throw new BusinessException("Unknown pricing method");
        }

        ChickenSaleItem item = new ChickenSaleItem();
        item.setFlock(lineFlock);
        item.setPricingMethod(method);
        item.setQuantity(req.getQuantity());
        item.setUnitPrice(req.getUnitPrice());
        item.setWeightKg(req.getWeightKg());
        item.setPricePerKg(req.getPricePerKg());
        item.setLineSubtotal(lineTotal.setScale(2, RoundingMode.HALF_UP));
        item.setNotes(req.getNotes());
        return item;
    }

    private void assertFlockSellable(Flock flock) {
        if (flock.getStatus() != FlockStatus.ACTIVE && flock.getStatus() != FlockStatus.PARTIALLY_SOLD) {
            throw new BusinessException("Flock " + flock.getBatchCode() + " is not sellable (status=" + flock.getStatus() + ")");
        }
    }

    private Barn resolveBarn(Long barnId, Flock flock, Farm farm) {
        if (barnId != null) {
            Barn barn = barnRepository.findById(barnId)
                    .orElseThrow(() -> new ResourceNotFoundException("Barn not found"));
            if (!barn.getFarm().getId().equals(farm.getId())) {
                throw new BusinessException("Barn does not belong to the selected farm");
            }
            if (flock.getBarn() != null && !flock.getBarn().getId().equals(barn.getId())) {
                throw new BusinessException("Selected barn does not match the flock barn");
            }
            return barn;
        }
        return flock.getBarn();
    }

    private void recordPopulationEvent(Flock flock, FlockPopulationEventType type, int previous, int qty,
                                       int remaining, Long saleId, String notes) {
        FlockPopulationEvent event = new FlockPopulationEvent();
        event.setFlock(flock);
        event.setEventType(type);
        event.setPreviousPopulation(previous);
        event.setQuantityChange(qty);
        event.setNewPopulation(remaining);
        event.setChickenSaleId(saleId);
        event.setEventDate(LocalDate.now());
        event.setRecordedBy(currentUser());
        event.setNotes(notes);
        populationEventRepository.save(event);
    }

    private ChickenSale findSale(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chicken sale not found: " + id));
    }

    private String nextSaleNumber() {
        String prefix = "CS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String nextInvoiceNumber() {
        return "CSINV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    ChickenSaleResponse toResponse(ChickenSale sale) {
        ChickenSaleResponse r = new ChickenSaleResponse();
        r.setId(sale.getId());
        r.setSaleNumber(sale.getSaleNumber());
        r.setFarmId(sale.getFarm().getId());
        r.setFarmName(sale.getFarm().getFarmName());
        if (sale.getBarn() != null) {
            r.setBarnId(sale.getBarn().getId());
            r.setBarnName(sale.getBarn().getName());
        }
        r.setFlockId(sale.getFlock().getId());
        r.setFlockBatchCode(sale.getFlock().getBatchCode());
        r.setFlockPopulation(sale.getFlock().getCurrentBirdCount());
        r.setCustomerId(sale.getCustomer().getId());
        r.setCustomerName(sale.getCustomer().getClientName());
        r.setSaleDate(sale.getSaleDate());
        r.setStatus(sale.getStatus());
        r.setSaleType(sale.getSaleType());
        r.setNotes(sale.getNotes());
        r.setSubtotal(sale.getSubtotal());
        r.setDiscountAmount(sale.getDiscountAmount());
        r.setTaxAmount(sale.getTaxAmount());
        r.setTotalAmount(sale.getTotalAmount());
        r.setPaidAmount(sale.getPaidAmount());
        r.setOutstandingAmount(sale.getOutstandingAmount());
        r.setTotalChickens(sale.totalChickens());
        if (sale.getInvoice() != null) {
            r.setInvoiceId(sale.getInvoice().getId());
            r.setInvoiceNumber(sale.getInvoice().getInvoiceNumber());
        }
        if (sale.getCreatedBy() != null) {
            r.setCreatedByName(sale.getCreatedBy().getFullName());
        }
        if (sale.getConfirmedBy() != null) {
            r.setConfirmedByName(sale.getConfirmedBy().getFullName());
        }
        r.setCreatedAt(sale.getCreatedAt());
        r.setUpdatedAt(sale.getUpdatedAt());
        r.setItems(sale.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
        if (sale.getId() != null) {
            r.setPayments(paymentRepository.findBySaleIdOrderByPaymentDateAsc(sale.getId())
                    .stream().map(this::toPaymentResponse).collect(Collectors.toList()));
            r.setPopulationEvents(populationEventRepository.findByChickenSaleIdOrderByCreatedAtAsc(sale.getId())
                    .stream().map(this::toEventResponse).collect(Collectors.toList()));
        }
        applyProfitability(r, sale);
        return r;
    }

    private void applyProfitability(ChickenSaleResponse r, ChickenSale sale) {
        r.setRevenue(sale.getTotalAmount() != null
                ? sale.getSubtotal().subtract(nvl(sale.getDiscountAmount()))
                : ZERO);
        r.setProfitabilityAvailable(false);
        r.setProfitabilityNote("Profitability is unavailable until flock cost allocation is fully implemented.");
        try {
            Map<String, Object> cogs = flockCOGSService.calculateCOGS(
                    sale.getFlock().getId(), sale.getFlock().getCurrentBirdCount());
            Object totalCost = cogs.get("totalCost");
            Object costPerBird = cogs.get("costPerBird");
            boolean hasCost = totalCost instanceof BigDecimal && ((BigDecimal) totalCost).compareTo(BigDecimal.ZERO) > 0;
            if (hasCost && costPerBird instanceof BigDecimal) {
                BigDecimal cogsAmount = ((BigDecimal) costPerBird)
                        .multiply(BigDecimal.valueOf(sale.totalChickens()))
                        .setScale(2, RoundingMode.HALF_UP);
                r.setCostOfGoodsSold(cogsAmount);
                r.setProfit(r.getRevenue().subtract(cogsAmount));
                r.setProfitabilityAvailable(true);
                r.setProfitabilityNote(null);
            }
        } catch (Exception ignored) {
            // leave unavailable
        }
    }

    private ChickenSaleItemResponse toItemResponse(ChickenSaleItem item) {
        ChickenSaleItemResponse r = new ChickenSaleItemResponse();
        r.setId(item.getId());
        r.setFlockId(item.getFlock().getId());
        r.setFlockBatchCode(item.getFlock().getBatchCode());
        r.setPricingMethod(item.getPricingMethod());
        r.setQuantity(item.getQuantity());
        r.setUnitPrice(item.getUnitPrice());
        r.setWeightKg(item.getWeightKg());
        r.setPricePerKg(item.getPricePerKg());
        r.setLineSubtotal(item.getLineSubtotal());
        r.setNotes(item.getNotes());
        return r;
    }

    private ChickenSalePaymentResponse toPaymentResponse(ChickenSalePayment p) {
        ChickenSalePaymentResponse r = new ChickenSalePaymentResponse();
        r.setId(p.getId());
        r.setInvoiceId(p.getInvoiceId());
        r.setAmount(p.getAmount());
        r.setPaymentDate(p.getPaymentDate());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setFinanceTransactionId(p.getFinanceTransactionId());
        r.setNotes(p.getNotes());
        r.setCreatedAt(p.getCreatedAt());
        if (p.getRecordedBy() != null) {
            r.setRecordedByName(p.getRecordedBy().getFullName());
        }
        return r;
    }

    private FlockPopulationEventResponse toEventResponse(FlockPopulationEvent e) {
        FlockPopulationEventResponse r = new FlockPopulationEventResponse();
        r.setId(e.getId());
        r.setEventType(e.getEventType());
        r.setPreviousPopulation(e.getPreviousPopulation());
        r.setQuantityChange(e.getQuantityChange());
        r.setNewPopulation(e.getNewPopulation());
        r.setChickenSaleId(e.getChickenSaleId());
        r.setEventDate(e.getEventDate());
        r.setNotes(e.getNotes());
        r.setCreatedAt(e.getCreatedAt());
        if (e.getRecordedBy() != null) {
            r.setRecordedByName(e.getRecordedBy().getFullName());
        }
        return r;
    }
}
