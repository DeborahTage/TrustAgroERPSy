package com.trustagro.inventory.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.procurement.service.ProcNumberService;
import com.trustagro.procurement.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final String MODULE = "INVENTORY";

    private final SupplierRepository supplierRepository;
    private final ProcNumberService procNumberService;
    private final AuditService auditService;

    public List<Supplier> getAll() {
        return supplierRepository.findAll();
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        ProcurementService.validatePaymentTerms(supplier.getPaymentTerms());
        if (supplier.getPaymentTerms() != null && !supplier.getPaymentTerms().isBlank()) {
            supplier.setPaymentTerms(ProcurementService.normalizePaymentTerms(supplier.getPaymentTerms()));
        }
        if (supplier.getCurrency() == null || supplier.getCurrency().isBlank()) {
            supplier.setCurrency("ETB");
        }
        if (supplier.getSupplierCode() == null || supplier.getSupplierCode().isBlank()) {
            supplier.setSupplierCode(procNumberService.nextSupplierCode());
        }
        return supplierRepository.save(supplier);
    }

    @Transactional
    public Supplier update(Long id, Supplier req) {
        Supplier s = getById(id);
        s.setName(req.getName());
        s.setContactPerson(req.getContactPerson());
        s.setEmail(req.getEmail());
        s.setPhone(req.getPhone());
        s.setAddress(req.getAddress());
        s.setCategory(req.getCategory());
        if (req.getPaymentTerms() != null) {
            ProcurementService.validatePaymentTerms(req.getPaymentTerms());
            s.setPaymentTerms(req.getPaymentTerms().isBlank()
                    ? req.getPaymentTerms()
                    : ProcurementService.normalizePaymentTerms(req.getPaymentTerms()));
        }
        s.setRating(req.getRating());
        if (req.getTaxIdentifier() != null) {
            s.setTaxIdentifier(req.getTaxIdentifier());
        }
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            s.setCurrency(req.getCurrency());
        }
        s.setActive(req.isActive());
        if (req.getNotes() != null) {
            s.setNotes(req.getNotes());
        }
        if ((s.getSupplierCode() == null || s.getSupplierCode().isBlank())
                && (req.getSupplierCode() == null || req.getSupplierCode().isBlank())) {
            s.setSupplierCode(procNumberService.nextSupplierCode());
        } else if (req.getSupplierCode() != null && !req.getSupplierCode().isBlank()
                && (s.getSupplierCode() == null || s.getSupplierCode().isBlank())) {
            s.setSupplierCode(req.getSupplierCode());
        }
        return supplierRepository.save(s);
    }

    /** Soft-delete only — never hard-delete suppliers that may be referenced by POs/GRNs. */
    @Transactional
    public void delete(Long id) {
        Supplier s = getById(id);
        s.setActive(false);
        supplierRepository.save(s);
        auditService.log("DEACTIVATE", MODULE, "Supplier", id, s.getName(), "active=false");
    }
}
