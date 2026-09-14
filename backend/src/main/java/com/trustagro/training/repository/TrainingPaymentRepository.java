package com.trustagro.training.repository;

import com.trustagro.training.entity.TrainingPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingPaymentRepository extends JpaRepository<TrainingPayment, Long> {
    List<TrainingPayment> findByRegistrationIdOrderByPaymentDateAsc(Long registrationId);
}
