package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FarmAccessServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks FarmAccessService service;

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void explicitlyAuthorizedSupervisoryRolesHaveGlobalFarmAccess() {
        for (Role supervisoryRole : List.of(Role.ADMIN, Role.GENERAL_MANAGER, Role.OPERATIONS_MANAGER)) {
            User user = user(supervisoryRole, null);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

            assertTrue(service.hasGlobalFarmAccess(user), supervisoryRole + " must have global farm access");
            assertTrue(service.canAccessFarm(user, 1L));
            assertTrue(service.canAccessFarm(user, 999L));
            assertNull(service.accessibleFarmIdsOrNull());
            assertDoesNotThrow(() -> service.assertCanAccessFarm(user, 999L));
            assertDoesNotThrow(() -> service.assertCanAccessFarm(999L));
        }
    }

    @Test
    void farmWorkerWithEmptyAssignmentsHasNoFarmAccess() {
        User worker = user(Role.FARM_WORKER, List.of());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(worker));

        assertFalse(service.hasGlobalFarmAccess(worker));
        assertFalse(service.canAccessFarm(worker, 1L));
        assertEquals(List.of(), service.accessibleFarmIdsOrNull());
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(worker, 1L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(1L));
    }

    @Test
    void farmWorkerWithNullAssignmentsHasNoFarmAccess() {
        User worker = user(Role.FARM_WORKER, null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(worker));

        assertFalse(service.hasGlobalFarmAccess(worker));
        assertFalse(service.canAccessFarm(worker, 1L));
        assertEquals(List.of(), service.accessibleFarmIdsOrNull());
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(worker, 1L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(1L));
    }

    @Test
    void farmManagerWithEmptyAssignmentsHasNoFarmAccess() {
        User mgr = user(Role.FARM_MANAGER, List.of());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mgr));

        assertFalse(service.hasGlobalFarmAccess(mgr));
        assertFalse(service.canAccessFarm(mgr, 1L));
        assertEquals(List.of(), service.accessibleFarmIdsOrNull());
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(mgr, 1L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(1L));
    }

    @Test
    void farmManagerWithNullAssignmentsHasNoFarmAccess() {
        User mgr = user(Role.FARM_MANAGER, null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mgr));

        assertFalse(service.hasGlobalFarmAccess(mgr));
        assertFalse(service.canAccessFarm(mgr, 1L));
        assertEquals(List.of(), service.accessibleFarmIdsOrNull());
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(mgr, 1L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(1L));
    }

    @Test
    void userAssignedToOneFarmCanOnlyAccessThatFarm() {
        User user = user(Role.FARM_WORKER, List.of(42L));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertFalse(service.hasGlobalFarmAccess(user));
        assertTrue(service.canAccessFarm(user, 42L));
        assertFalse(service.canAccessFarm(user, 43L));
        assertEquals(List.of(42L), service.accessibleFarmIdsOrNull());

        assertDoesNotThrow(() -> service.assertCanAccessFarm(42L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(43L));
    }

    @Test
    void userAssignedToMultipleFarmsCanAccessAllAssignedFarms() {
        User mgr = user(Role.FARM_MANAGER, List.of(10L, 20L, 30L));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mgr));

        assertFalse(service.hasGlobalFarmAccess(mgr));
        assertTrue(service.canAccessFarm(mgr, 10L));
        assertTrue(service.canAccessFarm(mgr, 20L));
        assertTrue(service.canAccessFarm(mgr, 30L));
        assertFalse(service.canAccessFarm(mgr, 40L));
        assertEquals(List.of(10L, 20L, 30L), service.accessibleFarmIdsOrNull());

        assertDoesNotThrow(() -> service.assertCanAccessFarm(10L));
        assertDoesNotThrow(() -> service.assertCanAccessFarm(20L));
        assertDoesNotThrow(() -> service.assertCanAccessFarm(30L));
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(40L));
    }

    private User user(Role role, List<Long> farmIds) {
        User u = new User();
        u.setEmail("user@test.com");
        u.setRole(role);
        u.setFarmIds(farmIds);
        setId(u, 7L);
        return u;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
