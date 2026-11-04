package com.trustagro.feed.service;

import com.trustagro.feed.entity.Silo;
import com.trustagro.feed.repository.FeedDeliveryRepository;
import com.trustagro.feed.repository.FeedRecipeRepository;
import com.trustagro.feed.repository.SiloRepository;
import com.trustagro.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceReconciliationTest {

    @Mock FeedRecipeRepository recipeRepository;
    @Mock SiloRepository siloRepository;
    @Mock FeedDeliveryRepository deliveryRepository;
    @Mock NotificationService notificationService;

    private FeedService service;

    @BeforeEach
    void setUp() {
        service = new FeedService(recipeRepository, siloRepository, deliveryRepository, notificationService);
    }

    @Test
    void sameSiloReconciliationAppliesOnlyConsumptionDifference() {
        Silo silo = silo(1L, 10.0);
        when(siloRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(silo));

        service.reconcileConsumption(1L, new BigDecimal("4"), 1L, new BigDecimal("6"));

        assertEquals(8.0, silo.getCurrentLevel());
        verify(siloRepository).save(silo);
    }

    @Test
    void movingRecordConsumptionRestoresOldSiloAndDeductsNewSilo() {
        Silo oldSilo = silo(1L, 2.0);
        Silo newSilo = silo(2L, 10.0);
        when(siloRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(oldSilo));
        when(siloRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(newSilo));

        service.reconcileConsumption(1L, new BigDecimal("3"), 2L, new BigDecimal("4"));

        assertEquals(5.0, oldSilo.getCurrentLevel());
        assertEquals(6.0, newSilo.getCurrentLevel());
        verify(siloRepository).findByIdForUpdate(1L);
        verify(siloRepository).findByIdForUpdate(2L);
        verify(siloRepository, times(2)).save(any(Silo.class));
    }

    @Test
    void rejectsConsumptionThatWouldMakeSiloNegative() {
        Silo silo = silo(1L, 2.0);
        when(siloRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(silo));

        assertThrows(IllegalStateException.class, () -> service.reconcileConsumption(
                null, BigDecimal.ZERO, 1L, new BigDecimal("3")));
        verify(siloRepository, never()).save(any(Silo.class));
    }

    private Silo silo(Long id, double currentLevel) {
        Silo silo = new Silo();
        silo.setId(id);
        silo.setName("Silo " + id);
        silo.setCurrentLevel(currentLevel);
        silo.setCapacity(100.0);
        return silo;
    }
}
