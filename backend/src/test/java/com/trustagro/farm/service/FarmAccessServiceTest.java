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
        TestingAuthenticationToken auth = new TestingAuthenticationToken("mgr@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void globalAdminAlwaysHasAccess() {
        User admin = user(Role.ADMIN, null);
        when(userRepository.findByEmail("mgr@test.com")).thenReturn(Optional.of(admin));
        assertTrue(service.hasGlobalFarmAccess(admin));
        assertTrue(service.canAccessFarm(admin, 99L));
        assertNull(service.accessibleFarmIdsOrNull());
    }

    @Test
    void emptyFarmIdsMeansGlobalForFarmManager() {
        User mgr = user(Role.FARM_MANAGER, List.of());
        assertTrue(service.hasGlobalFarmAccess(mgr));
        assertTrue(service.canAccessFarm(mgr, 1L));
    }

    @Test
    void scopedFarmManagerLimitedToAssignedFarms() {
        User mgr = user(Role.FARM_MANAGER, List.of(1L, 2L));
        when(userRepository.findByEmail("mgr@test.com")).thenReturn(Optional.of(mgr));
        assertFalse(service.hasGlobalFarmAccess(mgr));
        assertTrue(service.canAccessFarm(mgr, 1L));
        assertFalse(service.canAccessFarm(mgr, 3L));
        assertEquals(List.of(1L, 2L), service.accessibleFarmIdsOrNull());
        assertThrows(BusinessException.class, () -> service.assertCanAccessFarm(3L));
    }

    private User user(Role role, List<Long> farmIds) {
        User u = new User();
        u.setEmail("mgr@test.com");
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
