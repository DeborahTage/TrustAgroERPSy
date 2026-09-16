package com.trustagro.audit.repository;

import com.trustagro.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    long countByActionAndCreatedAtGreaterThanEqual(String action, LocalDateTime since);

    /** Filter-only search. Avoids LOWER/CONCAT so PostgreSQL does not bind unused keyword as bytea. */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "a.createdAt >= :startDate AND a.createdAt <= :endDate")
    Page<AuditLog> search(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("entityId") Long entityId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "a.createdAt >= :startDate AND a.createdAt <= :endDate AND " +
           "(LOWER(COALESCE(a.userEmail, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(COALESCE(a.entityType, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<AuditLog> searchWithKeyword(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("entityId") Long entityId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:module IS NULL OR a.module = :module) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "a.createdAt >= :startDate AND a.createdAt <= :endDate " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findWithFilters(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(a.module = 'SECURITY' OR a.action IN :actions) AND " +
           "a.createdAt >= :startDate AND a.createdAt <= :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findSecurityEvents(
            @Param("actions") Collection<String> actions,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions " +
           "ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByActions(@Param("actions") Collection<String> actions, Pageable pageable);
}
