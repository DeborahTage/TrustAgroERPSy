package com.trustagro.farm.document.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.delivery.entity.ChickenDeliveryRelease;
import com.trustagro.farm.delivery.entity.InternalStockDelivery;
import com.trustagro.farm.delivery.repository.ChickenDeliveryReleaseRepository;
import com.trustagro.farm.delivery.repository.InternalStockDeliveryRepository;
import com.trustagro.farm.document.entity.DocumentTemplate;
import com.trustagro.farm.document.repository.DocumentTemplateRepository;
import com.trustagro.farm.entity.DailyFarmRecord;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FlockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmDocumentService {

    private static final String ORG = "Trust Agro";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final FlockRepository flockRepository;
    private final DailyFarmRecordRepository dailyFarmRecordRepository;
    private final InternalStockDeliveryRepository internalDeliveryRepository;
    private final ChickenDeliveryReleaseRepository releaseRepository;
    private final DocumentTemplateRepository templateRepository;

    @PostConstruct
    @Transactional
    public void seedInactiveTemplates() {
        seed("BATCH_REGISTRATION", "Batch Registration Form", "FARM");
        seed("DAILY_FARM_REPORT", "Daily Farm Report", "FARM");
        seed("INTERNAL_DELIVERY", "Internal Stock Delivery Note", "FARM");
        seed("PAYMENT_VERIFICATION", "Payment Verification Form", "FARM");
        seed("CHICKEN_DISPATCH", "Chicken Dispatch Note", "FARM");
    }

    private void seed(String code, String name, String module) {
        if (templateRepository.findByCode(code).isEmpty()) {
            DocumentTemplate t = new DocumentTemplate();
            t.setCode(code);
            t.setName(name);
            t.setVersion(1);
            t.setActive(false);
            t.setModule(module);
            templateRepository.save(t);
        }
    }

    @Transactional(readOnly = true)
    public String render(String type, Long id) {
        String normalized = type == null ? "" : type.trim().toUpperCase().replace('-', '_');
        return switch (normalized) {
            case "BATCH_REGISTRATION" -> batchRegistration(id);
            case "DAILY_FARM_REPORT" -> dailyFarmReport(id);
            case "INTERNAL_DELIVERY" -> internalDelivery(id);
            case "PAYMENT_VERIFICATION" -> paymentVerification(id);
            case "CHICKEN_DISPATCH" -> chickenDispatch(id);
            default -> throw new BusinessException("Unknown farm document type: " + type);
        };
    }

    private String batchRegistration(Long id) {
        Flock flock = flockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        return html("Batch Registration", List.of(
                row("Batch code", flock.getBatchCode()),
                row("Farm", flock.getFarm() != null ? flock.getFarm().getFarmName() : ""),
                row("Bird type", nullToEmpty(flock.getBirdType())),
                row("Breed", nullToEmpty(flock.getBreed())),
                row("Initial birds", String.valueOf(flock.getInitialBirdCount())),
                row("Start date", formatDate(flock.getStartDate())),
                row("Supplier", flock.getSupplier() != null ? flock.getSupplier().getName() : ""),
                row("Responsible", flock.getResponsiblePerson() != null ? flock.getResponsiblePerson().getFullName() : ""),
                row("Notes", nullToEmpty(flock.getNotes()))
        ), List.of("Received by", "Farm Manager", "Operations"));
    }

    private String dailyFarmReport(Long id) {
        DailyFarmRecord rec = dailyFarmRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily farm record not found"));
        return html("Daily Farm Report", List.of(
                row("Date", formatDate(rec.getDate())),
                row("Farm", rec.getFarm().getFarmName()),
                row("Batch", rec.getFlock().getBatchCode()),
                row("Opening birds", String.valueOf(rec.getOpeningBirdCount())),
                row("Mortality", String.valueOf(rec.getMortality())),
                row("Culled", String.valueOf(rec.getCulledChickens())),
                row("Feed consumed", String.valueOf(rec.getFeedConsumed())),
                row("Water consumed", String.valueOf(rec.getWaterConsumed())),
                row("Remarks", nullToEmpty(rec.getSymptomsOrRemarks())),
                row("Recorded by", rec.getRecordedBy() != null ? rec.getRecordedBy().getFullName() : "")
        ), List.of("Recorded by", "Farm Manager"));
    }

    private String internalDelivery(Long id) {
        InternalStockDelivery d = internalDeliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internal delivery not found"));
        return html("Internal Stock Delivery", List.of(
                row("Receipt No.", d.getReceiptNumber()),
                row("Date", formatDate(d.getDeliveryDate())),
                row("Item", d.getInventoryItem() != null ? d.getInventoryItem().getItemName() : ""),
                row("SKU", d.getInventoryItem() != null ? d.getInventoryItem().getSku() : ""),
                row("Quantity", d.getQuantity() + " " + nullToEmpty(d.getUnit())),
                row("From", nullToEmpty(d.getSourceLocation())),
                row("To", nullToEmpty(d.getDestinationLocation())),
                row("Purpose", nullToEmpty(d.getPurpose())),
                row("Status", d.getStatus().name()),
                row("Notes", nullToEmpty(d.getNotes()))
        ), List.of("Deliverer", "Receiver", "Approved by"));
    }

    private String paymentVerification(Long id) {
        ChickenDeliveryRelease r = releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chicken release not found"));
        return html("Payment Verification", List.of(
                row("Release No.", r.getReleaseNumber()),
                row("Sale", r.getChickenSale() != null ? r.getChickenSale().getSaleNumber() : ""),
                row("Customer", r.getCustomer() != null ? r.getCustomer().getClientName() : ""),
                row("Farm", r.getFarm() != null ? r.getFarm().getFarmName() : ""),
                row("Payment amount", String.valueOf(r.getPaymentAmount())),
                row("Authorized qty", String.valueOf(r.getAuthorizedChickenQuantity())),
                row("Bonus qty", String.valueOf(r.getBonusQuantity())),
                row("Status", r.getStatus().name()),
                row("Verified by", r.getVerifiedBy() != null ? r.getVerifiedBy().getFullName() : ""),
                row("Receipt", nullToEmpty(r.getPaymentReceiptUrl()))
        ), List.of("Finance", "Farm Manager"));
    }

    private String chickenDispatch(Long id) {
        ChickenDeliveryRelease r = releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chicken release not found"));
        return html("Chicken Dispatch Note", List.of(
                row("Release No.", r.getReleaseNumber()),
                row("Dispatch date", formatDate(r.getDispatchDate())),
                row("Customer", r.getCustomer() != null ? r.getCustomer().getClientName() : ""),
                row("Farm", r.getFarm() != null ? r.getFarm().getFarmName() : ""),
                row("Batch", r.getFlock() != null ? r.getFlock().getBatchCode() : ""),
                row("Chicken type", nullToEmpty(r.getChickenType())),
                row("Authorized qty", String.valueOf(r.getAuthorizedChickenQuantity())),
                row("Bonus qty", String.valueOf(r.getBonusQuantity())),
                row("Dispatched by", r.getDispatchedBy() != null ? r.getDispatchedBy().getFullName() : ""),
                row("Status", r.getStatus().name())
        ), List.of("Dispatched by", "Driver / Receiver", "Marketing"));
    }

    private String html(String title, List<String> rows, List<String> signatures) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>")
                .append("<title>").append(esc(title)).append("</title>")
                .append("<style>")
                .append("@page{size:A4;margin:18mm}")
                .append("body{font-family:Georgia,'Times New Roman',serif;color:#1a1a1a;font-size:12pt}")
                .append("h1{font-size:18pt;margin:0 0 4px} .org{font-size:22pt;letter-spacing:.04em;margin-bottom:2px}")
                .append("table{width:100%;border-collapse:collapse;margin-top:16px}")
                .append("td{padding:6px 4px;border-bottom:1px solid #ccc;vertical-align:top}")
                .append("td.k{width:32%;font-weight:600} .sigs{margin-top:40px;display:flex;gap:24px}")
                .append(".sig{flex:1;border-top:1px solid #333;padding-top:6px;min-height:48px}")
                .append(".meta{color:#555;font-size:10pt;margin-top:4px}")
                .append("</style></head><body>");
        sb.append("<div class=\"org\">").append(ORG).append("</div>");
        sb.append("<h1>").append(esc(title)).append("</h1>");
        sb.append("<div class=\"meta\">Printed ").append(LocalDate.now().format(DATE_FMT)).append("</div>");
        sb.append("<table>");
        for (String row : rows) {
            sb.append(row);
        }
        sb.append("</table><div class=\"sigs\">");
        for (String sig : signatures) {
            sb.append("<div class=\"sig\">").append(esc(sig)).append("</div>");
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String row(String key, String value) {
        return "<tr><td class=\"k\">" + esc(key) + "</td><td>" + esc(value) + "</td></tr>";
    }

    private String formatDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
