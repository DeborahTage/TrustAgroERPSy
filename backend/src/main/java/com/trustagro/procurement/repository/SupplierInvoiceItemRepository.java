package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.SupplierInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierInvoiceItemRepository extends JpaRepository<SupplierInvoiceItem, Long> {
    List<SupplierInvoiceItem> findBySupplierInvoiceIdOrderByIdAsc(Long supplierInvoiceId);
    void deleteBySupplierInvoiceId(Long supplierInvoiceId);
}
