package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.AssignmentStatus;
import com.trustagro.consultancy.entity.ConsultancyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConsultancyAssignmentRepository extends JpaRepository<ConsultancyAssignment, Long> {
    List<ConsultancyAssignment> findByEngagementId(Long engagementId);
    List<ConsultancyAssignment> findByConsultantIdAndStatusIn(Long consultantId, Collection<AssignmentStatus> statuses);
    boolean existsByEngagementIdAndConsultantIdAndStatusIn(Long engagementId, Long consultantId, Collection<AssignmentStatus> statuses);
}
