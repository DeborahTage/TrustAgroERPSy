package com.trustagro.farm.sale.repository;

import com.trustagro.farm.sale.entity.ChickenSalePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChickenSalePaymentRepository extends JpaRepository<ChickenSalePayment, Long> {
    List<ChickenSalePayment> findBySaleIdOrderByPaymentDateAsc(Long saleId);
}
