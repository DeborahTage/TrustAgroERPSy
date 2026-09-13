package com.trustagro.finance.service;

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

        Invoice saved = invoiceRepository.save(invoice);

        // Auto-post to finance
        AccountCode accountCode = getRevenueAccountForUnit(unit);
        financeService.postRevenue(subtotal, unit, accountCode.getCode(),
                "POS Invoice " + saved.getInvoiceNumber(), "INVOICE", "INVOICE", saved.getId());

        if (vatAmount.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(TransactionType.INCOME, vatAmount, unit,
                    "1000", AccountCode.LIABILITY_VAT.getCode(),
                    "VAT from Invoice " + saved.getInvoiceNumber(), "INVOICE", "INVOICE", saved.getId());
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
        invoice.setTotalAmount(amount); // Assuming service might be VAT exempt or included for now
        invoice.setBusinessUnit(unit);
        invoice.setDueDate(dueDate);
        
        return invoiceRepository.save(invoice);
    }
}
