package com.trustagro.materials.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.document.entity.DocumentTemplate;
import com.trustagro.farm.document.repository.DocumentTemplateRepository;
import com.trustagro.materials.entity.MaterialRequest;
import com.trustagro.materials.entity.MaterialRequestLine;
import com.trustagro.materials.repository.MaterialIssueEventRepository;
import com.trustagro.materials.repository.MaterialRequestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialDocumentService {

    private static final String ORG = "Trust Agro";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MaterialRequestRepository requestRepository;
    private final MaterialIssueEventRepository issueEventRepository;
    private final DocumentTemplateRepository templateRepository;

    @PostConstruct
    @Transactional
    public void seedInactiveTemplates() {
        seed("MATERIAL_REQUEST", "Material Request Form", "MATERIALS");
        seed("FINANCE_APPROVAL", "Finance Approval Note", "MATERIALS");
        seed("STORE_ISSUE", "Store Issue Note", "MATERIALS");
        seed("GOODS_RECEIPT_ACK", "Goods Receipt Acknowledgement", "MATERIALS");
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
            case "MATERIAL_REQUEST" -> materialRequest(id);
            case "FINANCE_APPROVAL" -> financeApproval(id);
            case "STORE_ISSUE" -> storeIssue(id);
            case "GOODS_RECEIPT_ACK" -> goodsReceiptAck(id);
            default -> throw new BusinessException("Unknown material document type: " + type);
        };
    }

    private String materialRequest(Long id) {
        MaterialRequest mr = findRequest(id);
        List<String> rows = new ArrayList<>(List.of(
                row("Request No.", mr.getRequestNumber()),
                row("Type", mr.getType().name()),
                row("Status", mr.getStatus().name()),
                row("Date", formatDate(mr.getRequestDate())),
                row("Needed by", formatDate(mr.getNeededBy())),
                row("Requester", mr.getRequester() != null ? mr.getRequester().getFullName() : ""),
                row("Department", nullToEmpty(mr.getDepartment())),
                row("Farm", mr.getFarm() != null ? mr.getFarm().getFarmName() : ""),
                row("Priority", nullToEmpty(mr.getPriority())),
                row("Reason", nullToEmpty(mr.getReason())),
                row("Estimated total", String.valueOf(mr.getEstimatedTotal()))
        ));
        for (MaterialRequestLine line : mr.getLines()) {
            rows.add(row("Line item",
                    (line.getInventoryItem() != null ? line.getInventoryItem().getItemName() : "")
                            + " x " + line.getQuantityRequested()
                            + " " + nullToEmpty(line.getUnit())));
        }
        return html("Material Request", rows, List.of("Requester", "Finance", "Store"));
    }

    private String financeApproval(Long id) {
        MaterialRequest mr = findRequest(id);
        return html("Finance Approval", List.of(
                row("Request No.", mr.getRequestNumber()),
                row("Status", mr.getStatus().name()),
                row("Approved amount", String.valueOf(mr.getApprovedAmount())),
                row("Approved by", mr.getFinanceApprovedBy() != null ? mr.getFinanceApprovedBy().getFullName() : ""),
                row("Approved at", mr.getFinanceApprovedAt() != null ? mr.getFinanceApprovedAt().toString() : ""),
                row("Reason / notes", nullToEmpty(mr.getRejectionReason()))
        ), List.of("Finance Officer", "Requester"));
    }

    private String storeIssue(Long id) {
        // id may be MaterialRequest or MaterialIssueEvent
        OptionalIssue issue = resolveIssue(id);
        MaterialRequest mr = issue.request;
        List<String> rows = new ArrayList<>(List.of(
                row("Issue No.", issue.issueNumber),
                row("Request No.", mr.getRequestNumber()),
                row("Farm", mr.getFarm() != null ? mr.getFarm().getFarmName() : ""),
                row("Status", mr.getStatus().name())
        ));
        for (MaterialRequestLine line : mr.getLines()) {
            rows.add(row("Issued item",
                    (line.getInventoryItem() != null ? line.getInventoryItem().getItemName() : "")
                            + " issued " + line.getQuantityIssued()
                            + " / approved " + line.getQuantityApproved()));
        }
        return html("Store Issue Note", rows, List.of("Store Keeper", "Receiver"));
    }

    private String goodsReceiptAck(Long id) {
        MaterialRequest mr = findRequest(id);
        return html("Goods Receipt Acknowledgement", List.of(
                row("Request No.", mr.getRequestNumber()),
                row("Status", mr.getStatus().name()),
                row("Acknowledged by", mr.getAcknowledgedBy() != null ? mr.getAcknowledgedBy().getFullName() : ""),
                row("Acknowledged at", mr.getAcknowledgedAt() != null ? mr.getAcknowledgedAt().toString() : ""),
                row("Notes", nullToEmpty(mr.getAcknowledgementNotes()))
        ), List.of("Farm Manager", "Store Keeper"));
    }

    private MaterialRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material request not found"));
    }

    private OptionalIssue resolveIssue(Long id) {
        return issueEventRepository.findById(id)
                .map(ev -> new OptionalIssue(ev.getMaterialRequest(), ev.getIssueNumber()))
                .orElseGet(() -> {
                    MaterialRequest mr = findRequest(id);
                    return new OptionalIssue(mr, mr.getRequestNumber());
                });
    }

    private record OptionalIssue(MaterialRequest request, String issueNumber) {}

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
