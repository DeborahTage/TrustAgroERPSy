package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findBySupplierInvoiceIdOrderByCreatedAtDesc(Long supplierInvoiceId);
    List<SupplierPayment> findAllByOrderByCreatedAtDesc();
}
