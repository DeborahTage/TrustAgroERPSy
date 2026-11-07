package com.trustagro.farm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.EligibleSiteManagerResponse;
import com.trustagro.farm.dto.SiteFarmManagerRequest;
import com.trustagro.farm.dto.SiteFarmManagerResponse;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.user.entity.OrganizationalDesignation;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FarmManagerService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final FarmAccessService farmAccessService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final String MODULE = "FARM_MANAGER";

    public void assertCanManageSiteManagers(User caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (farmAccessService.hasGlobalFarmAccess(caller)) {
            return;
        }
        if (caller.isGroupFarmManager()) {
            return;
        }
        if (caller.getRole() == Role.FARM_MANAGER && caller.getDesignation() != OrganizationalDesignation.SITE_FARM_MANAGER) {
            return;
        }
        throw new AccessDeniedException("Only Group Farm Managers and administrators can manage site farm managers");
    }

    public List<SiteFarmManagerResponse> getAllSiteManagers() {
        User caller = farmAccessService.currentUser();
        assertCanManageSiteManagers(caller);

        List<User> allUsers = userRepository.findAll();
        List<Long> callerFarms = caller.getFarmIds() != null ? caller.getFarmIds() : List.of();

        List<User> managers = allUsers.stream()
                .filter(u -> !u.getId().equals(caller.getId()))
                .filter(u -> u.getDesignation() != OrganizationalDesignation.GROUP_FARM_MANAGER)
                .filter(u -> {
                    boolean isSiteDesignation = u.getDesignation() == OrganizationalDesignation.SITE_FARM_MANAGER;
                    boolean isReporting = u.getReportsTo() != null && u.getReportsTo().getId().equals(caller.getId());
                    boolean hasFarmRole = u.getRole() == Role.FARM_MANAGER || u.getRole() == Role.FARM_WORKER;

                    if (farmAccessService.hasGlobalFarmAccess(caller)) {
                        return isSiteDesignation || isReporting || (hasFarmRole && u.getDesignation() == null);
                    }

                    boolean assignedToCallerFarm = u.getFarmIds() != null && u.getFarmIds().stream().anyMatch(callerFarms::contains);
                    return isSiteDesignation || isReporting || assignedToCallerFarm || (hasFarmRole && u.getDesignation() == null);
                })
                .collect(Collectors.toList());

        return managers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public SiteFarmManagerResponse createSiteFarmManager(SiteFarmManagerRequest req) {
        User caller = farmAccessService.currentUser();
        assertCanManageSiteManagers(caller);

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException("Password is required for new site farm manager");
        }
        if (req.getPassword().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already in use");
        }

        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(email);
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        // Security rule: ALWAYS create Role.FARM_MANAGER with SITE_FARM_MANAGER designation
        user.setRole(Role.FARM_MANAGER);
        user.setDesignation(OrganizationalDesignation.SITE_FARM_MANAGER);
        user.setStatus(UserStatus.ACTIVE);
        user.setReportsTo(caller.isGroupFarmManager() ? caller : caller);

        Farm assignedFarm = null;
        if (req.getAssignedFarmId() != null) {
            farmAccessService.assertCanAccessFarm(caller, req.getAssignedFarmId());
            assignedFarm = farmRepository.findById(req.getAssignedFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + req.getAssignedFarmId()));
            if (assignedFarm.getAssignedFarmManager() != null) {
                throw new BusinessException("Farm '" + assignedFarm.getFarmName() + "' is already assigned to manager " +
                        assignedFarm.getAssignedFarmManager().getFullName());
            }
            user.setFarmIds(new ArrayList<>(List.of(assignedFarm.getId())));
        } else {
            user.setFarmIds(new ArrayList<>());
        }

        User saved = userRepository.save(user);

        if (assignedFarm != null) {
            assignedFarm.setAssignedFarmManager(saved);
            farmRepository.save(assignedFarm);
        }

        auditService.logObject("CREATE", MODULE, "SITE_FARM_MANAGER", saved.getId(), null, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public SiteFarmManagerResponse updateSiteFarmManager(Long id, SiteFarmManagerRequest req) {
        User caller = farmAccessService.currentUser();
        assertCanManageSiteManagers(caller);

        if (caller.getId().equals(id)) {
            throw new BusinessException("Cannot modify your own manager account through this portal");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + id));

        if (target.getRole() != Role.FARM_MANAGER || target.getDesignation() != OrganizationalDesignation.SITE_FARM_MANAGER) {
            throw new BusinessException("Target user is not a site farm manager");
        }

        if (!farmAccessService.hasGlobalFarmAccess(caller)) {
            boolean reportsToCaller = target.getReportsTo() != null && target.getReportsTo().getId().equals(caller.getId());
            List<Long> callerFarms = caller.getFarmIds() != null ? caller.getFarmIds() : List.of();
            boolean assignedToCallerFarm = target.getFarmIds() != null && target.getFarmIds().stream().anyMatch(callerFarms::contains);
            if (!reportsToCaller && !assignedToCallerFarm) {
                throw new AccessDeniedException("Not authorized to manage this site farm manager");
            }
        }

        SiteFarmManagerResponse before = toResponse(target);

        target.setFullName(req.getFullName().trim());
        target.setPhone(req.getPhone());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 8) {
                throw new BusinessException("Password must be at least 8 characters");
            }
            target.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        // Handle Farm assignment change & duplicate prevention
        Optional<Farm> currentAssignedFarm = farmRepository.findByAssignedFarmManagerId(id);
        Long currentFarmId = currentAssignedFarm.map(Farm::getId).orElse(null);
        Long newFarmId = req.getAssignedFarmId();

        if (!Objects.equals(currentFarmId, newFarmId)) {
            if (newFarmId != null) {
                farmAccessService.assertCanAccessFarm(caller, newFarmId);
                Farm newFarm = farmRepository.findById(newFarmId)
                        .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + newFarmId));
                if (newFarm.getAssignedFarmManager() != null && !newFarm.getAssignedFarmManager().getId().equals(id)) {
                    throw new BusinessException("Farm '" + newFarm.getFarmName() + "' is already assigned to manager " +
                            newFarm.getAssignedFarmManager().getFullName());
                }
                currentAssignedFarm.ifPresent(cf -> {
                    cf.setAssignedFarmManager(null);
                    farmRepository.save(cf);
                });
                newFarm.setAssignedFarmManager(target);
                farmRepository.save(newFarm);
                target.setFarmIds(new ArrayList<>(List.of(newFarm.getId())));
            } else {
                currentAssignedFarm.ifPresent(cf -> {
                    cf.setAssignedFarmManager(null);
                    farmRepository.save(cf);
                });
                target.setFarmIds(new ArrayList<>());
            }
        }

        User saved = userRepository.save(target);
        auditService.logObject("UPDATE", MODULE, "SITE_FARM_MANAGER", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public SiteFarmManagerResponse updateStatus(Long id, UserStatus status) {
        User caller = farmAccessService.currentUser();
        assertCanManageSiteManagers(caller);

        if (caller.getId().equals(id)) {
            throw new BusinessException("Cannot change status of your own account");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + id));

        if (target.getRole() != Role.FARM_MANAGER || target.getDesignation() != OrganizationalDesignation.SITE_FARM_MANAGER) {
            throw new BusinessException("Target user is not a site farm manager");
        }

        if (!farmAccessService.hasGlobalFarmAccess(caller)) {
            boolean reportsToCaller = target.getReportsTo() != null && target.getReportsTo().getId().equals(caller.getId());
            List<Long> callerFarms = caller.getFarmIds() != null ? caller.getFarmIds() : List.of();
            boolean assignedToCallerFarm = target.getFarmIds() != null && target.getFarmIds().stream().anyMatch(callerFarms::contains);
            if (!reportsToCaller && !assignedToCallerFarm) {
                throw new AccessDeniedException("Not authorized to manage this site farm manager");
            }
        }

        target.setStatus(status);
        if (status == UserStatus.INACTIVE) {
            userRepository.incrementTokenVersion(id);
        }

        User saved = userRepository.save(target);
        auditService.log("STATUS_CHANGE", MODULE, "SITE_FARM_MANAGER", saved.getId(), null, status.name());
        return toResponse(saved);
    }

    public List<EligibleSiteManagerResponse> getEligibleSiteManagers(Long farmId) {
        User caller = null;
        try {
            caller = farmAccessService.currentUser();
        } catch (Exception ignored) {
            // Unauthenticated or system runner
        }

        List<User> allUsers = userRepository.findAll();
        final User finalCaller = caller;

        return allUsers.stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getRole() == Role.FARM_MANAGER || u.getRole() == Role.FARM_WORKER || u.getDesignation() == OrganizationalDesignation.SITE_FARM_MANAGER)
                .filter(u -> u.getDesignation() != OrganizationalDesignation.GROUP_FARM_MANAGER)
                .filter(u -> {
                    if (finalCaller != null && finalCaller.getId().equals(u.getId())) {
                        boolean isGroupMgr = finalCaller.isGroupFarmManager() ||
                                (finalCaller.getRole() == Role.FARM_MANAGER && finalCaller.getDesignation() != OrganizationalDesignation.SITE_FARM_MANAGER);
                        return !isGroupMgr;
                    }
                    return true;
                })
                .map(u -> {
                    Optional<Farm> assignedOpt = farmRepository.findByAssignedFarmManagerId(u.getId());
                    Long assignedId = assignedOpt.map(Farm::getId).orElse(null);
                    String assignedName = assignedOpt.map(Farm::getFarmName).orElse(null);

                    boolean isEligible = assignedId == null || (farmId != null && assignedId.equals(farmId));

                    EligibleSiteManagerResponse r = new EligibleSiteManagerResponse();
                    r.setId(u.getId());
                    r.setFullName(u.getFullName());
                    r.setEmail(u.getEmail());
                    r.setPhone(u.getPhone());
                    r.setDesignation(u.getDesignation() != null ? u.getDesignation() : OrganizationalDesignation.SITE_FARM_MANAGER);
                    r.setAssignedFarmId(assignedId);
                    r.setAssignedFarmName(assignedName);
                    r.setEligible(isEligible);
                    return r;
                })
                .collect(Collectors.toList());
    }

    public SiteFarmManagerResponse toResponse(User user) {
        SiteFarmManagerResponse r = new SiteFarmManagerResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setDesignation(user.getDesignation());
        r.setStatus(user.getStatus());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedAt(user.getUpdatedAt());

        if (user.getReportsTo() != null) {
            r.setReportsToId(user.getReportsTo().getId());
            r.setReportsToName(user.getReportsTo().getFullName());
        }

        farmRepository.findByAssignedFarmManagerId(user.getId()).ifPresent(f -> {
            r.setAssignedFarmId(f.getId());
            r.setAssignedFarmName(f.getFarmName());
            r.setAssignedFarmLocation(f.getLocation());
        });

        return r;
    }
}
