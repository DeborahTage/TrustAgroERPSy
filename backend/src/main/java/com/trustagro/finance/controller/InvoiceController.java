package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','GENERAL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Invoice>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAllInvoices()));
    }

    @PostMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE', 'INVENTORY_MANAGER', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Invoice>> createPOS(@RequestBody Map<String, Object> req) {
        String clientName = req.containsKey("clientName")
                ? req.get("clientName").toString()
                : req.getOrDefault("customerName", "Walk-in Customer").toString();
        Long clientId = req.get("clientId") != null ? Long.valueOf(req.get("clientId").toString()) : null;
        com.trustagro.finance.entity.BusinessUnit unit = req.containsKey("unit")
                ? com.trustagro.finance.entity.BusinessUnit.valueOf(req.get("unit").toString())
                : com.trustagro.finance.entity.BusinessUnit.PHARMACY;

        BigDecimal subtotal;
        BigDecimal vatAmount;
        if (req.containsKey("subtotal")) {
            subtotal = new BigDecimal(req.get("subtotal").toString());
            vatAmount = req.containsKey("vatAmount")
                    ? new BigDecimal(req.get("vatAmount").toString())
                    : BigDecimal.ZERO;
        } else {
            BigDecimal total = new BigDecimal(req.get("amount").toString());
            subtotal = total.divide(new BigDecimal("1.15"), 2, RoundingMode.HALF_UP);
            vatAmount = total.subtract(subtotal);
        }

        Invoice invoice = invoiceService.createPOSInvoice(clientId, clientName, unit, subtotal, vatAmount);
        return ResponseEntity.ok(ApiResponse.success("POS Invoice created", invoice));
    }

    @PostMapping("/service")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE', 'GENERAL_MANAGER')")
    public ResponseEntity<ApiResponse<Invoice>> createService(@RequestBody Map<String, Object> req) {
        Invoice invoice = invoiceService.createServiceInvoice(
                Long.valueOf(req.get("clientId").toString()),
                req.get("clientName").toString(),
                com.trustagro.finance.entity.BusinessUnit.valueOf(req.get("unit").toString()),
                new BigDecimal(req.get("amount").toString()),
                LocalDate.parse(req.get("dueDate").toString())
        );
        return ResponseEntity.ok(ApiResponse.success("Service Invoice created", invoice));
    }
}
