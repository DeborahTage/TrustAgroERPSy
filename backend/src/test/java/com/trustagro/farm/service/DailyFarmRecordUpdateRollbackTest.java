package com.trustagro.farm.service;

import com.trustagro.farm.dto.DailyFarmRecordRequest;
import com.trustagro.farm.entity.DailyFarmRecord;
import com.trustagro.farm.entity.Barn;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.feed.entity.Silo;
import com.trustagro.feed.repository.SiloRepository;
import com.trustagro.farm.repository.BarnRepository;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.dto.DailyMedicationLineRequest;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.MovementType;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.entity.StockMovement;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
class DailyFarmRecordUpdateRollbackTest {

    @Autowired DailyFarmRecordService service;
    @Autowired DailyFarmRecordRepository recordRepository;
    @Autowired FarmRepository farmRepository;
    @Autowired FlockRepository flockRepository;
    @Autowired BarnRepository barnRepository;
    @Autowired SiloRepository siloRepository;
    @Autowired InventoryItemRepository inventoryItemRepository;
    @Autowired StockBatchRepository stockBatchRepository;
    @Autowired StockMovementRepository stockMovementRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockBean FarmAccessService farmAccessService;

    private DailyFarmRecord record;
    private Flock flock;
    private Silo silo;

    @BeforeEach
    void setUp() {
        doNothing().when(farmAccessService).assertCanAccessFarm(anyLong());

        Farm farm = new Farm();
        farm.setFarmName("Update rollback " + UUID.randomUUID());
        farm = farmRepository.saveAndFlush(farm);

        flock = new Flock();
        flock.setBatchCode("UPDATE-" + UUID.randomUUID());
        flock.setFarm(farm);
        flock.setInitialBirdCount(3);
        flock.setCurrentBirdCount(2);
        flock = flockRepository.saveAndFlush(flock);

        Barn barn = new Barn();
        barn.setName("Rollback barn");
        barn.setFarm(farm);
        barn = barnRepository.saveAndFlush(barn);
        silo = new Silo();
        silo.setName("Rollback silo");
        silo.setBarn(barn);
        silo.setCapacity(100.0);
        silo.setCurrentLevel(5.0);
        silo = siloRepository.saveAndFlush(silo);

        record = new DailyFarmRecord();
        record.setDate(LocalDate.now());
        record.setFarm(farm);
        record.setFlock(flock);
        record.setMortality(1);
        record.setCulledChickens(0);
        record.setSilo(silo);
        record.setFeedConsumed(new BigDecimal("3"));
        record = recordRepository.saveAndFlush(record);
    }

    @Test
    void rejectedUpdateRollsBackManagedRecordAndFlockChanges() {
        DailyFarmRecordRequest request = new DailyFarmRecordRequest();
        request.setDate(record.getDate());
        request.setFarmId(record.getFarm().getId());
        request.setFlockId(flock.getId());
        request.setMortality(4);
        request.setCulledChickens(0);

        assertThrows(RuntimeException.class, () -> service.update(record.getId(), request));

        DailyFarmRecord persistedRecord = recordRepository.findById(record.getId()).orElseThrow();
        Flock persistedFlock = flockRepository.findById(flock.getId()).orElseThrow();
        assertEquals(1, persistedRecord.getMortality());
        assertEquals(2, persistedFlock.getCurrentBirdCount());
    }

    @Test
    void failedInventoryEditRollsBackRecordStockAndAuditMovements() {
        InventoryItem item = new InventoryItem();
        item.setSku("ROLLBACK-" + UUID.randomUUID());
        item.setItemName("Medication");
        item.setCurrentStock(8.0);
        item = inventoryItemRepository.saveAndFlush(item);

        StockBatch batch = new StockBatch();
        batch.setItem(item);
        batch.setBatchNumber("B-" + UUID.randomUUID());
        batch.setQuantityReceived(10.0);
        batch.setQuantityRemaining(8.0);
        batch.setReceivedDate(LocalDate.now());
        batch = stockBatchRepository.saveAndFlush(batch);

        StockMovement priorIssue = new StockMovement();
        priorIssue.setItem(item);
        priorIssue.setBatch(batch);
        priorIssue.setMovementType(MovementType.ISSUE);
        priorIssue.setQuantity(2.0);
        priorIssue.setReferenceType("DAILY_FARM_RECORD");
        priorIssue.setReferenceId(record.getId());
        stockMovementRepository.saveAndFlush(priorIssue);

        DailyMedicationLineRequest medication = new DailyMedicationLineRequest();
        medication.setInventoryItemId(item.getId());
        medication.setQuantity(new BigDecimal("100"));
        DailyFarmRecordRequest request = new DailyFarmRecordRequest();
        request.setDate(record.getDate());
        request.setFarmId(record.getFarm().getId());
        request.setFlockId(flock.getId());
        request.setMortality(1);
        request.setFeedConsumed(new BigDecimal("2"));
        request.setSiloId(silo.getId());
        request.setMedicationLines(List.of(medication));

        assertThrows(IllegalStateException.class, () -> service.update(record.getId(), request));

        assertEquals(8.0, inventoryItemRepository.findById(item.getId()).orElseThrow().getCurrentStock());
        assertEquals(8.0, stockBatchRepository.findById(batch.getId()).orElseThrow().getQuantityRemaining());
        assertEquals(5.0, siloRepository.findById(silo.getId()).orElseThrow().getCurrentLevel());
        assertEquals(1, stockMovementRepository.findByReferenceTypeAndReferenceId(
                "DAILY_FARM_RECORD", record.getId()).size());
        assertTrue(stockMovementRepository.findByReferenceTypeAndReferenceId(
                "DAILY_FARM_RECORD_REVERSAL", priorIssue.getId()).isEmpty());
        Integer medicationLines = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM daily_medication_lines WHERE daily_record_id = ?",
                Integer.class, record.getId());
        assertEquals(0, medicationLines);
        Integer reversalAudits = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE entity_type = 'DailyFarmRecord' "
                        + "AND entity_id = ? AND action = 'STOCK_OUT_REVERSED'",
                Integer.class, record.getId());
        assertEquals(0, reversalAudits);
    }

    @Test
    void repeatedSuccessfulUpdateDoesNotDuplicateMedicationOrSiloDeductions() {
        InventoryItem item = new InventoryItem();
        item.setSku("REPEAT-" + UUID.randomUUID());
        item.setItemName("Medication");
        item.setCurrentStock(8.0);
        item = inventoryItemRepository.saveAndFlush(item);

        StockBatch batch = new StockBatch();
        batch.setItem(item);
        batch.setBatchNumber("B-" + UUID.randomUUID());
        batch.setQuantityReceived(10.0);
        batch.setQuantityRemaining(8.0);
        batch.setReceivedDate(LocalDate.now());
        batch = stockBatchRepository.saveAndFlush(batch);
        createPriorIssue(item, batch);

        DailyMedicationLineRequest medication = new DailyMedicationLineRequest();
        medication.setInventoryItemId(item.getId());
        medication.setQuantity(BigDecimal.ONE);
        DailyFarmRecordRequest request = new DailyFarmRecordRequest();
        request.setDate(record.getDate());
        request.setFarmId(record.getFarm().getId());
        request.setFlockId(flock.getId());
        request.setMortality(1);
        request.setFeedConsumed(new BigDecimal("2"));
        request.setSiloId(silo.getId());
        request.setMedicationLines(List.of(medication));

        service.update(record.getId(), request);
        double stockAfterFirstUpdate = inventoryItemRepository.findById(item.getId())
                .orElseThrow().getCurrentStock();
        double siloAfterFirstUpdate = siloRepository.findById(silo.getId()).orElseThrow().getCurrentLevel();
        service.update(record.getId(), request);

        assertEquals(9.0, stockAfterFirstUpdate);
        assertEquals(9.0, inventoryItemRepository.findById(item.getId()).orElseThrow().getCurrentStock());
        assertEquals(9.0, stockBatchRepository.findById(batch.getId()).orElseThrow().getQuantityRemaining());
        assertEquals(6.0, siloAfterFirstUpdate);
        assertEquals(6.0, siloRepository.findById(silo.getId()).orElseThrow().getCurrentLevel());
        List<StockMovement> sourceMovements = stockMovementRepository.findByReferenceTypeAndReferenceId(
                "DAILY_FARM_RECORD", record.getId());
        assertEquals(3, sourceMovements.size());
        assertEquals(2, stockMovementRepository.findByReferenceTypeAndReferenceIdIn(
                "DAILY_FARM_RECORD_REVERSAL",
                sourceMovements.stream().map(StockMovement::getId).toList()).size());
        Integer medLines = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM daily_medication_lines WHERE daily_record_id = ?",
                Integer.class, record.getId());
        assertEquals(1, medLines);
    }

    private StockMovement createPriorIssue(InventoryItem item, StockBatch batch) {
        StockMovement issue = new StockMovement();
        issue.setItem(item);
        issue.setBatch(batch);
        issue.setMovementType(MovementType.ISSUE);
        issue.setQuantity(2.0);
        issue.setReferenceType("DAILY_FARM_RECORD");
        issue.setReferenceId(record.getId());
        return stockMovementRepository.saveAndFlush(issue);
    }
}
