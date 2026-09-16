package com.trustagro.hr.repository;

import com.trustagro.hr.entity.EmployeeProfile;
import com.trustagro.hr.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
    Optional<EmployeeProfile> findByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumber(String employeeNumber);
    Optional<EmployeeProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<EmployeeProfile> findByEmploymentStatus(EmploymentStatus status);
    List<EmployeeProfile> findByEmploymentStatusIn(Collection<EmploymentStatus> statuses);
    List<EmployeeProfile> findByDepartmentId(Long departmentId);
    List<EmployeeProfile> findByManagerId(Long managerId);
    long countByEmploymentStatus(EmploymentStatus status);
    long countByDepartmentId(Long departmentId);
    long countByPositionId(Long positionId);

    @Query("SELECT e FROM EmployeeProfile e ORDER BY e.employeeNumber ASC")
    List<EmployeeProfile> findAllOrdered();

    @Query("SELECT e.department.name, COUNT(e) FROM EmployeeProfile e WHERE e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countGroupedByDepartment();

    @Query("SELECT e FROM EmployeeProfile e WHERE e.employmentStatus IN :statuses ORDER BY e.employeeNumber ASC")
    List<EmployeeProfile> findPayrollEligible(@Param("statuses") Collection<EmploymentStatus> statuses);
}
