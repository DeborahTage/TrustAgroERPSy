package com.trustagro.farm.repository;

import com.trustagro.farm.entity.DailyFarmRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyFarmRecordRepository extends JpaRepository<DailyFarmRecord, Long> {
    List<DailyFarmRecord> findByFarmId(Long farmId);
    List<DailyFarmRecord> findByFlockId(Long flockId);
    boolean existsByFarmIdAndFlockIdAndDate(Long farmId, Long flockId, LocalDate date);
    Optional<DailyFarmRecord> findTopByFlockIdOrderByDateDesc(Long flockId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyFarmRecord d WHERE d.id = :id")
    Optional<DailyFarmRecord> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(d.mortality), 0) FROM DailyFarmRecord d WHERE d.date = :date")
    Integer sumMortalityByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(d.mortality), 0) FROM DailyFarmRecord d WHERE d.date BETWEEN :start AND :end")
    Integer sumMortalityBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(d.mortality), 0) FROM DailyFarmRecord d WHERE d.flock.id = :flockId AND d.date BETWEEN :start AND :end")
    Integer sumMortalityBetweenAndFlock(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("flockId") Long flockId);

    @Query("SELECT d.date, COALESCE(SUM(d.mortality), 0) FROM DailyFarmRecord d " +
            "WHERE d.date BETWEEN :start AND :end GROUP BY d.date ORDER BY d.date")
    List<Object[]> sumMortalityGroupedByDate(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(d.mortality), 0) FROM DailyFarmRecord d WHERE d.flock.id = :flockId")
    Integer sumMortalityByFlock(@Param("flockId") Long flockId);

    List<DailyFarmRecord> findByFlockIdAndDateBetweenOrderByDateDesc(Long flockId, LocalDate start, LocalDate end);
}
