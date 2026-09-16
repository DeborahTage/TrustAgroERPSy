package com.trustagro.masterdata.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.masterdata.dto.MasterDataRequest;
import com.trustagro.masterdata.entity.MasterDataCategory;
import com.trustagro.masterdata.entity.MasterDataItem;
import com.trustagro.masterdata.repository.MasterDataItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataService {

    private final MasterDataItemRepository repository;
    private final AuditService auditService;
    private static final String MODULE = "MDM";

    public List<MasterDataItem> getAllByCategory(MasterDataCategory category, boolean activeOnly) {
        if (activeOnly) {
            return repository.findByCategoryAndActiveTrueOrderBySortOrderAscValueAsc(category);
        }
        return repository.findByCategoryOrderBySortOrderAscValueAsc(category);
    }

    public MasterDataItem create(MasterDataRequest request) {
        if (repository.existsByCategoryAndValue(request.getCategory(), request.getValue())) {
            throw new BusinessException("A master data item with this category and value already exists.");
        }

        MasterDataItem item = new MasterDataItem();
        item.setCategory(request.getCategory());
        item.setValue(request.getValue());
        item.setLabel(request.getLabel());
        item.setDescription(request.getDescription());
        item.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        item.setActive(request.isActive());

        MasterDataItem saved = repository.save(item);
        auditService.logObject("CREATE", MODULE, "MASTER_DATA", saved.getId(), null, saved);
        return saved;
    }

    public MasterDataItem update(Long id, MasterDataRequest request) {
        MasterDataItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Master data item not found"));

        if (!item.getValue().equals(request.getValue()) && 
            repository.existsByCategoryAndValue(request.getCategory(), request.getValue())) {
            throw new BusinessException("A master data item with this category and value already exists.");
        }

        String before = item.getCategory() + ":" + item.getValue() + ":" + item.getLabel() + ":" + item.isActive();

        item.setCategory(request.getCategory());
        item.setValue(request.getValue());
        item.setLabel(request.getLabel());
        item.setDescription(request.getDescription());
        item.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        item.setActive(request.isActive());

        MasterDataItem saved = repository.save(item);
        auditService.log("UPDATE", MODULE, "MASTER_DATA", saved.getId(), before, 
                saved.getCategory() + ":" + saved.getValue() + ":" + saved.getLabel() + ":" + saved.isActive());
        return saved;
    }

    public void delete(Long id) {
        MasterDataItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Master data item not found"));
        item.setActive(false);
        repository.save(item);
        auditService.log("DEACTIVATE", MODULE, "MASTER_DATA", id,
                item.getCategory() + ":" + item.getValue(), "active=false");
    }

    public MasterDataItem toggleStatus(Long id) {
        MasterDataItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Master data item not found"));
        item.setActive(!item.isActive());
        MasterDataItem saved = repository.save(item);
        auditService.log("TOGGLE_STATUS", MODULE, "MASTER_DATA", id, 
            String.valueOf(!item.isActive()), String.valueOf(item.isActive()));
        return saved;
    }
}
