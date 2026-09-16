package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.SupplierInvoice;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long> {
    Optional<SupplierInvoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsBySupplierIdAndSupplierInvoiceNumber(Long supplierId, String supplierInvoiceNumber);
    List<SupplierInvoice> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    List<SupplierInvoice> findByStatusOrderByCreatedAtDesc(SupplierInvoiceStatus status);
    List<SupplierInvoice> findAllByOrderByCreatedAtDesc();
    long countByStatus(SupplierInvoiceStatus status);
}
