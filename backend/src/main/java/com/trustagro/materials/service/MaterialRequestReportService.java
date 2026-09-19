package com.trustagro.materials.service;

import com.trustagro.materials.entity.MaterialRequest;
import com.trustagro.materials.entity.MaterialRequestStatus;
import com.trustagro.materials.entity.MaterialRequestType;
import com.trustagro.materials.repository.MaterialRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialRequestReportService {

    private final MaterialRequestRepository requestRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> summary(LocalDate from, LocalDate to, MaterialRequestType type,
                                             MaterialRequestStatus status, Long farmId, String department) {
        return requestRepository.filter(status, type, farmId, null, from, to, department).stream()
                .map(this::row)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> byStatus(LocalDate from, LocalDate to, MaterialRequestType type, Long farmId) {
        List<MaterialRequest> rows = requestRepository.filter(null, type, farmId, null, from, to, null);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (MaterialRequestStatus s : MaterialRequestStatus.values()) {
            counts.put(s.name(), 0L);
        }
        for (MaterialRequest mr : rows) {
            counts.merge(mr.getStatus().name(), 1L, Long::sum);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from);
        out.put("to", to);
        out.put("counts", counts);
        out.put("total", rows.size());
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> byDepartment(LocalDate from, LocalDate to, MaterialRequestType type, Long farmId) {
        List<MaterialRequest> rows = requestRepository.filter(null, type, farmId, null, from, to, null);
        Map<String, Long> counts = rows.stream().collect(Collectors.groupingBy(
                mr -> mr.getDepartment() != null ? mr.getDepartment() : "UNASSIGNED",
                LinkedHashMap::new,
                Collectors.counting()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("byDepartment", counts);
        out.put("total", rows.size());
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> fulfillment(LocalDate from, LocalDate to, Long farmId) {
        List<MaterialRequest> rows = requestRepository.filter(null, null, farmId, null, from, to, null);
        long fulfilled = rows.stream().filter(r ->
                r.getStatus() == MaterialRequestStatus.FULFILLED
                        || r.getStatus() == MaterialRequestStatus.ACKNOWLEDGED).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", rows.size());
        out.put("fulfilled", fulfilled);
        out.put("fulfillmentRate", rows.isEmpty() ? 0.0 : (fulfilled * 100.0 / rows.size()));
        return out;
    }

    private Map<String, Object> row(MaterialRequest mr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mr.getId());
        m.put("requestNumber", mr.getRequestNumber());
        m.put("type", mr.getType().name());
        m.put("status", mr.getStatus().name());
        m.put("department", mr.getDepartment());
        m.put("farmId", mr.getFarm() != null ? mr.getFarm().getId() : null);
        m.put("farmName", mr.getFarm() != null ? mr.getFarm().getFarmName() : null);
        m.put("requester", mr.getRequester() != null ? mr.getRequester().getFullName() : null);
        m.put("requestDate", mr.getRequestDate());
        m.put("neededBy", mr.getNeededBy());
        m.put("priority", mr.getPriority());
        m.put("estimatedTotal", mr.getEstimatedTotal());
        m.put("approvedAmount", mr.getApprovedAmount());
        m.put("paidAmount", mr.getPaidAmount());
        return m;
    }
}
