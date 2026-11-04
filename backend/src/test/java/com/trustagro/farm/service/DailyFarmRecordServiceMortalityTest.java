package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.dto.DailyFarmRecordRequest;
import com.trustagro.farm.dto.DailyMortalityLineRequest;
import com.trustagro.farm.entity.DailyFarmRecord;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.feed.entity.Silo;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.feed.service.FeedService;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.repository.DiseaseCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyFarmRecordServiceMortalityTest {

    @Mock DailyFarmRecordRepository recordRepository;
    @Mock FarmRepository farmRepository;
    @Mock FlockRepository flockRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock FeedService feedService;
    @Mock DiseaseCaseRepository diseaseCaseRepository;
    @Mock FarmAccessService farmAccessService;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryService inventoryService;

    @InjectMocks DailyFarmRecordService service;

    private Farm farm;
    private Flock flock;
    private final AtomicLong ids = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("fm@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setEmail("fm@test.com");
        setId(user, 1L);

        farm = new Farm();
        farm.setFarmName("Main");
        setId(farm, 1L);

        flock = new Flock();
        flock.setBatchCode("B-1");
        flock.setFarm(farm);
        flock.setCurrentBirdCount(100);
        flock.setStartDate(LocalDate.now().minusDays(14));
        setId(flock, 10L);

        ReflectionTestUtils.setField(service, "mortalityThreshold", 5.0);

        lenient().doNothing().when(farmAccessService).assertCanAccessFarm(anyLong());
        lenient().when(farmAccessService.currentUser()).thenReturn(user);
        lenient().when(userRepository.findByEmail("fm@test.com")).thenReturn(Optional.of(user));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));
        lenient().when(flockRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(flock));
        lenient().when(recordRepository.existsByFarmIdAndFlockIdAndDate(any(), any(), any())).thenReturn(false);
        lenient().when(recordRepository.save(any())).thenAnswer(inv -> {
            DailyFarmRecord r = inv.getArgument(0);
            if (r.getId() == null) setId(r, ids.getAndIncrement());
            return r;
        });
        lenient().when(flockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mortalityLinesSumAndDecrementFlockOnce() {
        DailyMortalityLineRequest m1 = new DailyMortalityLineRequest();
        m1.setDeathCount(3);
        DailyMortalityLineRequest m2 = new DailyMortalityLineRequest();
        m2.setDeathCount(2);

        DailyFarmRecordRequest req = new DailyFarmRecordRequest();
        req.setDate(LocalDate.now());
        req.setFarmId(1L);
        req.setFlockId(10L);
        req.setOpeningBirdCount(100);
        req.setCulledChickens(1);
        req.setMortalityLines(List.of(m1, m2));

        var res = service.create(req);

        assertEquals(5, res.getMortality());
        assertEquals(94, flock.getCurrentBirdCount());
        assertEquals(2, res.getMortalityLines().size());
        verify(flockRepository, times(1)).save(flock);
    }

    @Test
    void rejectsMortalityExceedingAvailable() {
        DailyMortalityLineRequest m1 = new DailyMortalityLineRequest();
        m1.setDeathCount(80);
        DailyMortalityLineRequest m2 = new DailyMortalityLineRequest();
        m2.setDeathCount(30);

        DailyFarmRecordRequest req = new DailyFarmRecordRequest();
        req.setDate(LocalDate.now());
        req.setFarmId(1L);
        req.setFlockId(10L);
        req.setOpeningBirdCount(100);
        req.setMortalityLines(List.of(m1, m2));

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void updateReconcilesPreviousAndNewFlockAndSiloEffects() {
        Flock nextFlock = new Flock();
        nextFlock.setBatchCode("B-2");
        nextFlock.setFarm(farm);
        nextFlock.setCurrentBirdCount(50);
        nextFlock.setStartDate(LocalDate.now().minusDays(7));
        setId(nextFlock, 11L);

        DailyFarmRecord record = existingRecord(3, 1, 8, 30L, flock);
        when(recordRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(record));
        when(flockRepository.findAllByIdForUpdate(List.of(10L, 11L))).thenReturn(List.of(flock, nextFlock));

        DailyFarmRecordRequest req = updateRequest(11L, 2, 2, 4, 20L);
        service.update(50L, req);

        assertEquals(104, flock.getCurrentBirdCount());
        assertEquals(46, nextFlock.getCurrentBirdCount());
        assertEquals(11L, record.getFlock().getId());
        assertEquals(new java.math.BigDecimal("4"), record.getFeedConsumed());
        verify(feedService).reconcileConsumption(30L, new java.math.BigDecimal("8"),
                20L, new java.math.BigDecimal("4"));
        verify(inventoryService).reverseStockOutsForReference(
                "DAILY_FARM_RECORD", 50L, "DAILY_FARM_RECORD_REVERSAL", farmAccessService.currentUser());
    }

    @Test
    void repeatedUpdateReplacesPriorFlockEffectInsteadOfApplyingItTwice() {
        DailyFarmRecord record = existingRecord(2, 1, 5, 30L, flock);
        when(recordRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(record));
        when(flockRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(flock));
        DailyFarmRecordRequest req = updateRequest(10L, 3, 1, 5, 30L);

        service.update(50L, req);
        int afterFirstUpdate = flock.getCurrentBirdCount();
        service.update(50L, req);

        assertEquals(99, afterFirstUpdate);
        assertEquals(afterFirstUpdate, flock.getCurrentBirdCount());
        verify(feedService, times(2)).reconcileConsumption(
                30L, new java.math.BigDecimal("5"), 30L, new java.math.BigDecimal("5"));
        verify(inventoryService, times(2)).reverseStockOutsForReference(
                "DAILY_FARM_RECORD", 50L, "DAILY_FARM_RECORD_REVERSAL", farmAccessService.currentUser());
    }

    @Test
    void invalidUpdatedMortalityDoesNotReverseInventoryOrSiloEffects() {
        flock.setCurrentBirdCount(2);
        DailyFarmRecord record = existingRecord(1, 0, 5, 30L, flock);
        when(recordRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(record));
        when(flockRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(flock));

        assertThrows(BusinessException.class,
                () -> service.update(50L, updateRequest(10L, 4, 0, 3, 30L)));

        verifyNoInteractions(feedService, inventoryService);
    }

    private DailyFarmRecord existingRecord(int mortality, int culled, int consumed,
                                           Long siloId, Flock recordFlock) {
        DailyFarmRecord record = new DailyFarmRecord();
        setId(record, 50L);
        record.setDate(LocalDate.now());
        record.setFarm(farm);
        record.setFlock(recordFlock);
        record.setMortality(mortality);
        record.setCulledChickens(culled);
        record.setFeedConsumed(new java.math.BigDecimal(consumed));
        Silo silo = new Silo();
        silo.setId(siloId);
        record.setSilo(silo);
        return record;
    }

    private DailyFarmRecordRequest updateRequest(Long flockId, int mortality, int culled,
                                                 int consumed, Long siloId) {
        DailyFarmRecordRequest req = new DailyFarmRecordRequest();
        req.setDate(LocalDate.now());
        req.setFarmId(1L);
        req.setFlockId(flockId);
        req.setMortality(mortality);
        req.setCulledChickens(culled);
        req.setFeedConsumed(new java.math.BigDecimal(consumed));
        req.setSiloId(siloId);
        return req;
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
