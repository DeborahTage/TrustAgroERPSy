package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.dto.BiosecurityProtocolRequest;
import com.trustagro.farm.dto.FarmRequest;
import com.trustagro.farm.dto.FarmResponse;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.repository.BiosecurityProtocolRepository;
import com.trustagro.farm.repository.FarmRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmAccessEnforcementTest {

    @Mock FarmRepository farmRepository;
    @Mock BiosecurityProtocolRepository biosecurityRepo;
    @Mock UserRepository userRepository;

    private FarmAccessService farmAccessService;
    private FarmService farmService;

    private Farm farm1;
    private Farm farm2;

    @BeforeEach
    void setUp() {
        farmAccessService = new FarmAccessService(userRepository);
        farmService = new FarmService(farmRepository, userRepository, biosecurityRepo, farmAccessService);

        farm1 = new Farm();
        setId(farm1, 1L);
        farm1.setFarmName("Farm Alpha");
        farm1.setStatus(FarmStatus.ACTIVE);

        farm2 = new Farm();
        setId(farm2, 2L);
        farm2.setFarmName("Farm Beta");
        farm2.setStatus(FarmStatus.ACTIVE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(user.getEmail(), "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void farmManagerCannotReadUnassignedFarm() {
        User manager = user(101L, "fm@trustagro.test", Role.FARM_MANAGER, List.of(1L));
        authenticateAs(manager);

        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm1));

        // Reading assigned farm succeeds
        FarmResponse response = farmService.getById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());

        // Attempting to read unassigned farm fails with BusinessException
        BusinessException ex = assertThrows(BusinessException.class, () -> farmService.getById(2L));
        assertTrue(ex.getMessage().contains("Not authorized to access this farm"));
        verify(farmRepository, never()).findById(2L);
    }

    @Test
    void farmWorkerCannotMutateUnassignedFarm() {
        User worker = user(102L, "worker@trustagro.test", Role.FARM_WORKER, List.of(1L));
        authenticateAs(worker);

        FarmRequest updateReq = new FarmRequest();
        updateReq.setFarmName("Renamed Beta");

        // Attempting update on unassigned farm throws BusinessException
        BusinessException ex1 = assertThrows(BusinessException.class, () -> farmService.update(2L, updateReq));
        assertTrue(ex1.getMessage().contains("Not authorized to access this farm"));

        // Attempting status change on unassigned farm throws BusinessException
        BusinessException ex2 = assertThrows(BusinessException.class, () -> farmService.updateStatus(2L, FarmStatus.INACTIVE));
        assertTrue(ex2.getMessage().contains("Not authorized to access this farm"));

        // Attempting biosecurity log on unassigned farm throws BusinessException
        BiosecurityProtocolRequest bioReq = new BiosecurityProtocolRequest();
        bioReq.setFarmId(2L);
        BusinessException ex3 = assertThrows(BusinessException.class, () -> farmService.logBiosecurity(bioReq));
        assertTrue(ex3.getMessage().contains("Not authorized to access this farm"));

        // Attempting to read biosecurity logs of unassigned farm throws BusinessException
        BusinessException ex4 = assertThrows(BusinessException.class, () -> farmService.getBiosecurityLogs(2L));
        assertTrue(ex4.getMessage().contains("Not authorized to access this farm"));

        verify(farmRepository, never()).save(any(Farm.class));
        verify(biosecurityRepo, never()).save(any());
    }

    @Test
    void unassignedFarmManagerReceivesEmptyListOnGetAll() {
        User unassignedManager = user(103L, "unassigned@trustagro.test", Role.FARM_MANAGER, List.of());
        authenticateAs(unassignedManager);

        List<FarmResponse> results = farmService.getAll();
        assertNotNull(results);
        assertTrue(results.isEmpty(), "FARM_MANAGER with empty farmIds must receive an empty list");
        verify(farmRepository, never()).findAll();
        verify(farmRepository, never()).findByIdIn(anyList());
    }

    @Test
    void nullFarmIdsFarmWorkerReceivesEmptyListOnGetAll() {
        User unassignedWorker = user(104L, "worker_null@trustagro.test", Role.FARM_WORKER, null);
        authenticateAs(unassignedWorker);

        List<FarmResponse> results = farmService.getAll();
        assertNotNull(results);
        assertTrue(results.isEmpty(), "FARM_WORKER with null farmIds must receive an empty list");
        verify(farmRepository, never()).findAll();
        verify(farmRepository, never()).findByIdIn(anyList());
    }

    @Test
    void supervisoryAdminCanReadAndMutateAnyFarmGlobally() {
        User admin = user(1L, "admin@trustagro.test", Role.ADMIN, null);
        authenticateAs(admin);

        when(farmRepository.findAll()).thenReturn(List.of(farm1, farm2));
        when(farmRepository.findById(2L)).thenReturn(Optional.of(farm2));
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        // Global list succeeds
        List<FarmResponse> all = farmService.getAll();
        assertEquals(2, all.size());

        // Global read succeeds for any farm
        FarmResponse res2 = farmService.getById(2L);
        assertEquals(2L, res2.getId());

        // Global update succeeds for any farm
        FarmResponse updated = farmService.updateStatus(2L, FarmStatus.INACTIVE);
        assertEquals(FarmStatus.INACTIVE, updated.getStatus());
    }

    private User user(Long id, String email, Role role, List<Long> farmIds) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        u.setFarmIds(farmIds);
        setId(u, id);
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
