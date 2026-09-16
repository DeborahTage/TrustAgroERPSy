package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyActivityRepository extends JpaRepository<ConsultancyActivity, Long> {
    Optional<ConsultancyActivity> findByActivityNumber(String activityNumber);
    List<ConsultancyActivity> findByEngagementIdOrderByActivityDateDesc(Long engagementId);
}
