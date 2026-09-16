package com.trustagro.hr.repository;

import com.trustagro.hr.entity.PayrollPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollPaymentRepository extends JpaRepository<PayrollPayment, Long> {
    Optional<PayrollPayment> findByPaymentNumber(String paymentNumber);
    List<PayrollPayment> findByPayrollRunIdOrderByIdDesc(Long payrollRunId);
    List<PayrollPayment> findByPayrollItemIdOrderByIdDesc(Long payrollItemId);
}
