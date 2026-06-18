package com.trustagro.finance.repository;

import com.trustagro.finance.entity.FinanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceAuditLogRepository extends JpaRepository<FinanceAuditLog, Long> {
    List<FinanceAuditLog> findByTransactionIdOrderByPostedAtDesc(Long transactionId);
    List<FinanceAuditLog> findBySourceModuleOrderByPostedAtDesc(String sourceModule);
    List<FinanceAuditLog> findTop100ByOrderByPostedAtDesc();
}
