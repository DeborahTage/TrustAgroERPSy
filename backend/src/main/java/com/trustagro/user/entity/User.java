package com.trustagro.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "designation")
    private OrganizationalDesignation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_id")
    private User reportsTo;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public Long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Long tokenVersion) { this.tokenVersion = tokenVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<Long> getFarmIds() { return farmIds; }
    public void setFarmIds(List<Long> farmIds) { this.farmIds = farmIds; }

    public OrganizationalDesignation getDesignation() { return designation; }
    public void setDesignation(OrganizationalDesignation designation) { this.designation = designation; }
    public User getReportsTo() { return reportsTo; }
    public void setReportsTo(User reportsTo) { this.reportsTo = reportsTo; }

    public boolean isGroupFarmManager() {
        return role == Role.FARM_MANAGER && designation != OrganizationalDesignation.SITE_FARM_MANAGER;
    }

    public boolean isSiteFarmManager() {
        return role == Role.FARM_MANAGER && designation == OrganizationalDesignation.SITE_FARM_MANAGER;
    }

    /**
     * Farm IDs this user has access to. Empty list = access to all farms.
     * Used for Farm Manager and Extension Worker roles.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_farm_access", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "farm_id")
    private List<Long> farmIds;
}
