package com.trustagro.farm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.EligibleSiteManagerResponse;
import com.trustagro.farm.dto.SiteFarmManagerRequest;
import com.trustagro.farm.dto.SiteFarmManagerResponse;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.user.entity.OrganizationalDesignation;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmManagerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    private FarmAccessService farmAccessService;
    private FarmManagerService farmManagerService;

    private User groupManager;
    private User siteManagerBiyo;
    private User farmWorker;
    private User adminUser;
    private Farm biyoFarm;
    private Farm sheraFarm;
    private Farm dukemFarm;

    @BeforeEach
    void setUp() throws Exception {
        farmAccessService = new FarmAccessService(userRepository);
        farmManagerService = new FarmManagerService(
                userRepository, farmRepository, farmAccessService, passwordEncoder, auditService);

        groupManager = new User();
        setId(groupManager, 10L);
        groupManager.setEmail("groupmgr@trustagro.com");
        groupManager.setFullName("Group Farm Manager");
        groupManager.setRole(Role.FARM_MANAGER);
        groupManager.setDesignation(OrganizationalDesignation.GROUP_FARM_MANAGER);
        groupManager.setStatus(UserStatus.ACTIVE);
        groupManager.setFarmIds(new ArrayList<>(List.of(1L, 2L, 3L, 4L)));

        siteManagerBiyo = new User();
        setId(siteManagerBiyo, 20L);
        siteManagerBiyo.setEmail("biyo.mgr@trustagro.com");
        siteManagerBiyo.setFullName("Biyo Site Manager");
        siteManagerBiyo.setRole(Role.FARM_MANAGER);
        siteManagerBiyo.setDesignation(OrganizationalDesignation.SITE_FARM_MANAGER);
        siteManagerBiyo.setStatus(UserStatus.ACTIVE);
        siteManagerBiyo.setReportsTo(groupManager);
        siteManagerBiyo.setFarmIds(new ArrayList<>(List.of(1L)));

        farmWorker = new User();
        setId(farmWorker, 30L);
        farmWorker.setEmail("worker@trustagro.com");
        farmWorker.setFullName("Farm Worker");
        farmWorker.setRole(Role.FARM_WORKER);
        farmWorker.setStatus(UserStatus.ACTIVE);
        farmWorker.setFarmIds(new ArrayList<>(List.of(1L)));

        adminUser = new User();
        setId(adminUser, 1L);
        adminUser.setEmail("admin@trustagro.com");
        adminUser.setFullName("Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        biyoFarm = new Farm();
        setId(biyoFarm, 1L);
        biyoFarm.setFarmName("Biyo Farm");
        biyoFarm.setLocation("Biyo");
        biyoFarm.setStatus(FarmStatus.ACTIVE);
        biyoFarm.setAssignedFarmManager(siteManagerBiyo);

        sheraFarm = new Farm();
        setId(sheraFarm, 2L);
        sheraFarm.setFarmName("Shera Farm");
        sheraFarm.setLocation("Shera");
        sheraFarm.setStatus(FarmStatus.ACTIVE);

        dukemFarm = new Farm();
        setId(dukemFarm, 3L);
        dukemFarm.setFarmName("Dukem Farm");
        dukemFarm.setLocation("Dukem");
        dukemFarm.setStatus(FarmStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(user.getEmail(), "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGroupManagerCreatesSiteFarmManagerSuccess() {
        authenticateAs(groupManager);
        when(userRepository.existsByEmail("shera.mgr@trustagro.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("encoded_pass");
        when(farmRepository.findById(2L)).thenReturn(Optional.of(sheraFarm));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            setId(u, 21L);
            return u;
        });

        SiteFarmManagerRequest req = new SiteFarmManagerRequest();
        req.setFullName("Shera Site Manager");
        req.setEmail("shera.mgr@trustagro.com");
        req.setPhone("+251911334455");
        req.setPassword("StrongPassword123!");
        req.setAssignedFarmId(2L);

        SiteFarmManagerResponse resp = farmManagerService.createSiteFarmManager(req);

        assertNotNull(resp);
        assertEquals("Shera Site Manager", resp.getFullName());
        assertEquals("shera.mgr@trustagro.com", resp.getEmail());
        assertEquals(Role.FARM_MANAGER, resp.getRole());
        assertEquals(OrganizationalDesignation.SITE_FARM_MANAGER, resp.getDesignation());
        assertEquals(UserStatus.ACTIVE, resp.getStatus());
        assertEquals(10L, resp.getReportsToId());

        assertNotNull(sheraFarm.getAssignedFarmManager());
        assertEquals("Shera Site Manager", sheraFarm.getAssignedFarmManager().getFullName());
        verify(farmRepository).save(sheraFarm);
    }

    @Test
    void testSiteManagerForbiddenFromCreatingSiteManagers() {
        authenticateAs(siteManagerBiyo);

        SiteFarmManagerRequest req = new SiteFarmManagerRequest();
        req.setFullName("Another Manager");
        req.setEmail("another@trustagro.com");
        req.setPassword("Pass12345678");

        assertThrows(AccessDeniedException.class, () -> farmManagerService.createSiteFarmManager(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testFarmWorkerForbiddenFromManagingSiteManagers() {
        authenticateAs(farmWorker);

        assertThrows(AccessDeniedException.class, () -> farmManagerService.getAllSiteManagers());
    }

    @Test
    void testPreventDuplicateFarmAssignmentOnCreate() {
        authenticateAs(groupManager);
        when(userRepository.existsByEmail("dukem.mgr@trustagro.com")).thenReturn(false);
        // Farm 1 is Biyo, which already has siteManagerBiyo assigned!
        when(farmRepository.findById(1L)).thenReturn(Optional.of(biyoFarm));

        SiteFarmManagerRequest req = new SiteFarmManagerRequest();
        req.setFullName("Duplicate Manager");
        req.setEmail("dukem.mgr@trustagro.com");
        req.setPassword("Pass12345678");
        req.setAssignedFarmId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> farmManagerService.createSiteFarmManager(req));
        assertTrue(ex.getMessage().contains("already assigned"));
    }

    @Test
    void testCrossFarmAssignmentDeniedIfGroupManagerDoesNotSuperviseFarm() {
        authenticateAs(groupManager);
        // Farm 99 is not in groupManager's farmIds [1, 2, 3, 4]
        Farm unassignedFarm = new Farm();
        setId(unassignedFarm, 99L);
        unassignedFarm.setFarmName("Foreign Farm");

        SiteFarmManagerRequest req = new SiteFarmManagerRequest();
        req.setFullName("Foreign Manager");
        req.setEmail("foreign@trustagro.com");
        req.setPassword("Pass12345678");
        req.setAssignedFarmId(99L);

        assertThrows(BusinessException.class, () -> farmManagerService.createSiteFarmManager(req));
    }

    @Test
    void testPreventSelfModificationByGroupManager() {
        authenticateAs(groupManager);

        SiteFarmManagerRequest req = new SiteFarmManagerRequest();
        req.setFullName("Modified Name");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> farmManagerService.updateSiteFarmManager(10L, req));
        assertTrue(ex.getMessage().contains("Cannot modify your own"));
    }

    @Test
    void testDeactivateSiteManagerRevokesTokens() {
        authenticateAs(groupManager);
        when(userRepository.findById(20L)).thenReturn(Optional.of(siteManagerBiyo));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SiteFarmManagerResponse resp = farmManagerService.updateStatus(20L, UserStatus.INACTIVE);

        assertEquals(UserStatus.INACTIVE, resp.getStatus());
        verify(userRepository).incrementTokenVersion(20L);
    }

    @Test
    void testEligibleSiteManagersFilteringForFarmDropdown() {
        authenticateAs(groupManager);

        User unassignedMgr = new User();
        setId(unassignedMgr, 22L);
        unassignedMgr.setFullName("Unassigned Manager");
        unassignedMgr.setEmail("unassigned@trustagro.com");
        unassignedMgr.setRole(Role.FARM_MANAGER);
        unassignedMgr.setDesignation(OrganizationalDesignation.SITE_FARM_MANAGER);
        unassignedMgr.setStatus(UserStatus.ACTIVE);
        unassignedMgr.setReportsTo(groupManager);

        when(userRepository.findAll()).thenReturn(List.of(groupManager, siteManagerBiyo, unassignedMgr));

        when(farmRepository.findByAssignedFarmManagerId(20L)).thenReturn(Optional.of(biyoFarm));
        when(farmRepository.findByAssignedFarmManagerId(22L)).thenReturn(Optional.empty());

        // When creating a new farm (farmId == null):
        List<EligibleSiteManagerResponse> candidatesForNew = farmManagerService.getEligibleSiteManagers(null);
        assertEquals(2, candidatesForNew.size());

        EligibleSiteManagerResponse unassignedResp = candidatesForNew.stream()
                .filter(r -> r.getId().equals(22L)).findFirst().orElseThrow();
        assertTrue(unassignedResp.isEligible());
        assertNull(unassignedResp.getAssignedFarmId());

        EligibleSiteManagerResponse assignedResp = candidatesForNew.stream()
                .filter(r -> r.getId().equals(20L)).findFirst().orElseThrow();
        assertFalse(assignedResp.isEligible());
        assertEquals(1L, assignedResp.getAssignedFarmId());

        // When editing Biyo farm (farmId == 1L): both unassigned and currently assigned (siteManagerBiyo) are eligible!
        List<EligibleSiteManagerResponse> candidatesForBiyo = farmManagerService.getEligibleSiteManagers(1L);
        assertEquals(2, candidatesForBiyo.size());
        assertTrue(candidatesForBiyo.stream().allMatch(EligibleSiteManagerResponse::isEligible));
    }

    @Test
    void testAdminHasGlobalManagementAccess() {
        authenticateAs(adminUser);
        when(userRepository.findAll()).thenReturn(List.of(adminUser, siteManagerBiyo));

        List<SiteFarmManagerResponse> all = farmManagerService.getAllSiteManagers();
        assertEquals(1, all.size());
        assertEquals("Biyo Site Manager", all.get(0).getFullName());
    }
}
