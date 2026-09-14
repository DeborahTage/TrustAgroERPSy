package com.trustagro.training.repository;

import com.trustagro.training.entity.ProgramStatus;
import com.trustagro.training.entity.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    Optional<TrainingProgram> findByProgramCode(String programCode);
    boolean existsByProgramCode(String programCode);
    List<TrainingProgram> findByStatusOrderByCreatedAtDesc(ProgramStatus status);
    long countByStatus(ProgramStatus status);
}
