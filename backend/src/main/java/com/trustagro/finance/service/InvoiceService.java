package com.trustagro.finance.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final FinanceService financeService;

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional
    public Invoice createPOSInvoice(Long clientId, String clientName, BusinessUnit unit,
                                   BigDecimal subtotal, BigDecimal vatAmount) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("POS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setInvoiceType(InvoiceType.POS);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setClientId(clientId);
        invoice.setClientName(clientName);
        invoice.setSubtotal(subtotal);
        invoice.setVatAmount(vatAmount);
        invoice.setTotalAmount(subtotal.add(vatAmount));
        invoice.setPaidAmount(subtotal.add(vatAmount));
        invoice.setBusinessUnit(unit);
        invoice.setDueDate(LocalDate.now());
        invoice.setSourceModule(FinanceSourceTypes.MODULE_INVOICE);

        Invoice saved = invoiceRepository.save(invoice);
        invoice.setSourceId(saved.getId());
        invoice.setSourceType(FinanceSourceTypes.REF_INVOICE_REVENUE);
        saved = invoiceRepository.save(invoice);

        AccountCode accountCode = getRevenueAccountForUnit(unit);
        // Revenue recognition only on subtotal — VAT is liability, not income
        financeService.postTransaction(TransactionType.INCOME, subtotal, unit,
                AccountCode.ASSET_CASH.getCode(), accountCode.getCode(),
                "POS Invoice " + saved.getInvoiceNumber(),
                FinanceSourceTypes.MODULE_INVOICE, FinanceSourceTypes.REF_INVOICE_REVENUE, saved.getId());

        if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(TransactionType.TRANSFER, vatAmount, unit,
                    AccountCode.ASSET_CASH.getCode(), AccountCode.LIABILITY_VAT.getCode(),
                    "VAT from Invoice " + saved.getInvoiceNumber(),
                    FinanceSourceTypes.MODULE_INVOICE, FinanceSourceTypes.REF_INVOICE_VAT, saved.getId());
        }

        return saved;
    }

    private AccountCode getRevenueAccountForUnit(BusinessUnit unit) {
        return switch (unit) {
            case POULTRY_FARM -> AccountCode.REV_POULTRY_SALES;
            case PHARMACY -> AccountCode.REV_PHARMACY_DRUGS;
            case CONSULTING -> AccountCode.REV_CONSULTING;
            case TRAINING -> AccountCode.REV_TRAINING;
            default -> AccountCode.REV_OTHER;
        };
    }

    @Transactional
    public Invoice createServiceInvoice(Long clientId, String clientName, BusinessUnit unit,
                                       BigDecimal amount, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("SRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setInvoiceType(InvoiceType.SERVICE);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(clientId);
        invoice.setClientName(clientName);
        invoice.setSubtotal(amount);
        invoice.setTotalAmount(amount);
        invoice.setBusinessUnit(unit);
        invoice.setDueDate(dueDate);
        invoice.setSourceModule(FinanceSourceTypes.MODULE_INVOICE);
        Invoice saved = invoiceRepository.save(invoice);
        // Service invoices recognize revenue on issue via AR (no cash until payment)
        financeService.postTransaction(TransactionType.INCOME, amount, unit,
                AccountCode.ASSET_RECEIVABLE.getCode(), getRevenueAccountForUnit(unit).getCode(),
                "Service invoice " + saved.getInvoiceNumber(),
                FinanceSourceTypes.MODULE_INVOICE, FinanceSourceTypes.REF_INVOICE_REVENUE, saved.getId());
        return saved;
    }

    @Transactional
    public Invoice cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            throw new BusinessException("Cannot cancel an invoice with payments; reverse payments first");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return invoice;
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getOpenReceivables() {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == InvoiceStatus.SENT
                        || i.getStatus() == InvoiceStatus.PARTIALLY_PAID
                        || i.getStatus() == InvoiceStatus.OVERDUE)
                .toList();
    }
}
