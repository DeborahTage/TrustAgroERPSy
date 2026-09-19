package com.trustagro.farm.importing.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.importing.dto.DataImportRequest;
import com.trustagro.farm.importing.dto.DataImportResult;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataImportServiceTest {

    @Mock FarmRepository farmRepository;
    @Mock FlockRepository flockRepository;
    @Mock InventoryItemRepository itemRepository;
    @Mock AuditService auditService;

    @InjectMocks DataImportService service;

    @Test
    void dryRunReportsDuplicateBatchWithoutPersisting() {
        Farm farm = new Farm();
        farm.setFarmName("Alpha");
        setId(farm, 1L);

        Flock existing = new Flock();
        existing.setBatchCode("BATCH-01");
        existing.setFarm(farm);
        setId(existing, 9L);

        when(farmRepository.findByFarmNameIgnoreCase("Alpha")).thenReturn(Optional.of(farm));
        when(flockRepository.findByBatchCodeIgnoreCase("BATCH-01")).thenReturn(Optional.of(existing));

        DataImportRequest req = new DataImportRequest();
        req.setType("BATCHES");
        req.setDryRun(true);
        req.setConfirm(false);
        req.setCsv("batchCode,farmName,birdType,breed,initialBirdCount,startDate\n"
                + "BATCH-01,Alpha,Broiler,Cobb,1000,2026-01-01\n");

        DataImportResult result = service.importData(req);

        assertEquals(1, result.getRowsFound());
        assertEquals(1, result.getValid());
        assertEquals(1, result.getDuplicates());
        assertEquals(1, result.getWillUpdate());
        assertEquals(0, result.getWillCreate());
        assertEquals(0, result.getCreated());
        assertEquals(4, result.getExpectedCounts().get("farmsExpected"));
        assertEquals(83, result.getExpectedCounts().get("batchesExpected"));
        verify(flockRepository, never()).save(any());
    }

    @Test
    void confirmRequiredWhenNotDryRun() {
        DataImportRequest req = new DataImportRequest();
        req.setType("FARMS");
        req.setDryRun(false);
        req.setConfirm(false);
        req.setCsv("farmName,location,farmType,capacity\nA,Loc,BROILER,1000\n");
        assertThrows(com.trustagro.common.exception.BusinessException.class, () -> service.importData(req));
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
