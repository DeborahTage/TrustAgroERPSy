package com.trustagro.materials.repository;

import com.trustagro.materials.entity.MaterialIssueEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialIssueEventRepository extends JpaRepository<MaterialIssueEvent, Long> {

    Optional<MaterialIssueEvent> findByIdempotencyKey(String idempotencyKey);

    Optional<MaterialIssueEvent> findByIssueNumber(String issueNumber);

    List<MaterialIssueEvent> findByMaterialRequestIdOrderByCreatedAtDesc(Long materialRequestId);
}
