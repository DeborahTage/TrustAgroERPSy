package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.security.RoleCompatibility;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Database-backed farm scope. Empty {@code User.farmIds} means global access
 * (main/global Farm Manager). Non-empty lists restrict FARM_MANAGER / FARM_WORKER /
 * EXTENSION_WORKER to assigned farms. Supervisory roles always have global access.
 */
@Service
@RequiredArgsConstructor
public class FarmAccessService {

    private final UserRepository userRepository;

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new BusinessException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    public boolean hasGlobalFarmAccess(User user) {
        if (user == null) {
            return false;
        }
        Role role = RoleCompatibility.normalize(user.getRole());
        if (role == Role.ADMIN || role == Role.GENERAL_MANAGER || role == Role.OPERATIONS_MANAGER) {
            return true;
        }
        List<Long> farmIds = user.getFarmIds();
        return farmIds == null || farmIds.isEmpty();
    }

    public boolean canAccessFarm(User user, Long farmId) {
        if (user == null || farmId == null) {
            return false;
        }
        if (hasGlobalFarmAccess(user)) {
            return true;
        }
        List<Long> farmIds = user.getFarmIds();
        return farmIds != null && farmIds.contains(farmId);
    }

    public void assertCanAccessFarm(Long farmId) {
        assertCanAccessFarm(currentUser(), farmId);
    }

    public void assertCanAccessFarm(User user, Long farmId) {
        if (!canAccessFarm(user, farmId)) {
            throw new BusinessException("Not authorized to access this farm");
        }
    }

    /**
     * @return {@code null} when the user may see all farms; otherwise the allowed IDs.
     */
    public List<Long> accessibleFarmIdsOrNull() {
        User user = currentUser();
        if (hasGlobalFarmAccess(user)) {
            return null;
        }
        List<Long> farmIds = user.getFarmIds();
        return farmIds == null ? List.of() : Collections.unmodifiableList(farmIds);
    }
}
