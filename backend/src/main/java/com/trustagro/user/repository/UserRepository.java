package com.trustagro.user.repository;

import com.trustagro.user.entity.OrganizationalDesignation;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    List<User> findByRoleAndDesignation(Role role, OrganizationalDesignation designation);
    List<User> findByReportsTo(User reportsTo);
    List<User> findByReportsToId(Long reportsToId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :id")
    int incrementTokenVersion(@Param("id") Long id);
}
