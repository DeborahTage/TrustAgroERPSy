package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmTask;
import com.trustagro.crm.entity.CrmTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmTaskRepository extends JpaRepository<CrmTask, Long> {
    List<CrmTask> findByClientIdOrderByDueDateAsc(Long clientId);
    List<CrmTask> findByStatusOrderByDueDateAsc(CrmTaskStatus status);
    List<CrmTask> findAllByOrderByDueDateAsc();
    List<CrmTask> findByAssignedUserIdOrderByDueDateAsc(Long userId);

    long countByStatus(CrmTaskStatus status);
}
