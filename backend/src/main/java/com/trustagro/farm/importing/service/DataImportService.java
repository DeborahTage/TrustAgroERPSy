package com.trustagro.farm.importing.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.FarmType;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.importing.dto.DataImportRequest;
import com.trustagro.farm.importing.dto.DataImportResult;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.ItemCategory;
import com.trustagro.inventory.entity.ItemStatus;
import com.trustagro.inventory.entity.ItemUnit;
import com.trustagro.inventory.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataImportService {

    public static final int FARMS_EXPECTED = 4;
    public static final int BATCHES_EXPECTED = 83;

    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;
    private final InventoryItemRepository itemRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Map<String, Object> templates() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("FARMS", "farmName,location,farmType,capacity");
        m.put("BATCHES", "batchCode,farmName,birdType,breed,initialBirdCount,startDate");
        m.put("INVENTORY_ITEMS", "itemCode,itemName,category,unit,reorderPoint");
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> onboardingSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("farmCount", farmRepository.count());
        m.put("flockCount", flockRepository.count());
        m.put("inventoryItemCount", itemRepository.count());
        m.put("farmsExpected", FARMS_EXPECTED);
        m.put("batchesExpected", BATCHES_EXPECTED);
        m.put("inventorySourceFilePresent", inventorySourceFilePresent());
        return m;
    }

    private boolean inventorySourceFilePresent() {
        List<Path> candidates = List.of(
                Path.of("SENE 30 INVENTORY.xlsx"),
                Path.of("../SENE 30 INVENTORY.xlsx"),
                Path.of("/home/kalilinux/Music/Trust ERP/SENE 30 INVENTORY.xlsx")
        );
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public DataImportResult importData(DataImportRequest req) {
        if (req.getType() == null || req.getType().isBlank()) {
            throw new BusinessException("Import type is required");
        }
        if (req.getCsv() == null) {
            throw new BusinessException("CSV content is required");
        }
        boolean dryRun = req.isDryRun();
        if (!dryRun && !req.isConfirm()) {
            throw new BusinessException("Confirm import requires dryRun=false AND confirm=true");
        }

        String type = req.getType().trim().toUpperCase(Locale.ROOT);
        DataImportResult result = new DataImportResult();
        result.setType(type);
        result.setDryRun(dryRun);
        result.getExpectedCounts().put("farmsExpected", FARMS_EXPECTED);
        result.getExpectedCounts().put("batchesExpected", BATCHES_EXPECTED);

        List<String[]> rows = parseCsv(req.getCsv());
        result.setRowsFound(Math.max(0, rows.size() - 1));

        switch (type) {
            case "FARMS" -> importFarms(rows, dryRun, result);
            case "BATCHES" -> importBatches(rows, dryRun, result);
            case "INVENTORY_ITEMS" -> importInventory(rows, dryRun, result);
            default -> throw new BusinessException("Unknown import type: " + type);
        }

        if (!dryRun) {
            auditService.log("IMPORT", "DATA_IMPORT", type, null,
                    "dryRun=false", "created=" + result.getCreated() + ",updated=" + result.getUpdated());
        }
        return result;
    }

    private void importFarms(List<String[]> rows, boolean dryRun, DataImportResult result) {
        if (rows.isEmpty()) return;
        Map<String, Integer> idx = headerIndex(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String farmName = cell(row, idx, "farmName");
            if (farmName == null || farmName.isBlank()) {
                result.setInvalid(result.getInvalid() + 1);
                result.getErrors().add("Row " + (i + 1) + ": farmName required");
                continue;
            }
            result.setValid(result.getValid() + 1);
            Optional<Farm> existing = farmRepository.findByFarmNameIgnoreCase(farmName);
            if (existing.isPresent()) {
                result.setDuplicates(result.getDuplicates() + 1);
                result.setWillUpdate(result.getWillUpdate() + 1);
                if (!dryRun) {
                    Farm f = existing.get();
                    applyFarm(f, row, idx);
                    farmRepository.save(f);
                    result.setUpdated(result.getUpdated() + 1);
                }
            } else {
                result.setWillCreate(result.getWillCreate() + 1);
                if (!dryRun) {
                    Farm f = new Farm();
                    applyFarm(f, row, idx);
                    f.setStatus(FarmStatus.ACTIVE);
                    farmRepository.save(f);
                    result.setCreated(result.getCreated() + 1);
                }
            }
        }
    }

    private void applyFarm(Farm f, String[] row, Map<String, Integer> idx) {
        f.setFarmName(cell(row, idx, "farmName"));
        f.setLocation(cell(row, idx, "location"));
        String type = cell(row, idx, "farmType");
        if (type != null && !type.isBlank()) {
            f.setFarmType(FarmType.valueOf(type.trim().toUpperCase(Locale.ROOT)));
        }
        String cap = cell(row, idx, "capacity");
        if (cap != null && !cap.isBlank()) {
            f.setCapacity(Integer.parseInt(cap.trim()));
        }
    }

    private void importBatches(List<String[]> rows, boolean dryRun, DataImportResult result) {
        if (rows.isEmpty()) return;
        Map<String, Integer> idx = headerIndex(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String batchCode = cell(row, idx, "batchCode");
            String farmName = cell(row, idx, "farmName");
            if (batchCode == null || batchCode.isBlank()) {
                result.setInvalid(result.getInvalid() + 1);
                result.getErrors().add("Row " + (i + 1) + ": batchCode required");
                continue;
            }
            Optional<Farm> farm = farmName == null || farmName.isBlank()
                    ? Optional.empty()
                    : farmRepository.findByFarmNameIgnoreCase(farmName);
            if (farm.isEmpty()) {
                result.setMissingRefs(result.getMissingRefs() + 1);
                result.setInvalid(result.getInvalid() + 1);
                result.getErrors().add("Row " + (i + 1) + ": farm not found: " + farmName);
                continue;
            }
            result.setValid(result.getValid() + 1);
            Optional<Flock> existing = flockRepository.findByBatchCodeIgnoreCase(batchCode);
            if (existing.isPresent()) {
                result.setDuplicates(result.getDuplicates() + 1);
                result.setWillUpdate(result.getWillUpdate() + 1);
                if (!dryRun) {
                    Flock flock = existing.get();
                    applyBatch(flock, row, idx, farm.get());
                    flockRepository.save(flock);
                    result.setUpdated(result.getUpdated() + 1);
                }
            } else {
                result.setWillCreate(result.getWillCreate() + 1);
                if (!dryRun) {
                    Flock flock = new Flock();
                    applyBatch(flock, row, idx, farm.get());
                    flock.setStatus(FlockStatus.ACTIVE);
                    flockRepository.save(flock);
                    result.setCreated(result.getCreated() + 1);
                }
            }
        }
    }

    private void applyBatch(Flock flock, String[] row, Map<String, Integer> idx, Farm farm) {
        flock.setBatchCode(cell(row, idx, "batchCode"));
        flock.setFarm(farm);
        flock.setBirdType(cell(row, idx, "birdType"));
        flock.setBreed(cell(row, idx, "breed"));
        String count = cell(row, idx, "initialBirdCount");
        if (count != null && !count.isBlank()) {
            int birds = Integer.parseInt(count.trim());
            flock.setInitialBirdCount(birds);
            if (flock.getCurrentBirdCount() == null) {
                flock.setCurrentBirdCount(birds);
            }
        }
        String start = cell(row, idx, "startDate");
        if (start != null && !start.isBlank()) {
            flock.setStartDate(LocalDate.parse(start.trim()));
        }
    }

    private void importInventory(List<String[]> rows, boolean dryRun, DataImportResult result) {
        if (rows.isEmpty()) return;
        Map<String, Integer> idx = headerIndex(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String code = cell(row, idx, "itemCode");
            if (code == null || code.isBlank()) {
                code = cell(row, idx, "sku");
            }
            String name = cell(row, idx, "itemName");
            if (code == null || code.isBlank() || name == null || name.isBlank()) {
                result.setInvalid(result.getInvalid() + 1);
                result.getErrors().add("Row " + (i + 1) + ": itemCode and itemName required");
                continue;
            }
            result.setValid(result.getValid() + 1);
            Optional<InventoryItem> existing = itemRepository.findBySku(code);
            if (existing.isPresent()) {
                result.setDuplicates(result.getDuplicates() + 1);
                result.setWillUpdate(result.getWillUpdate() + 1);
                if (!dryRun) {
                    InventoryItem item = existing.get();
                    applyItem(item, row, idx, code);
                    itemRepository.save(item);
                    result.setUpdated(result.getUpdated() + 1);
                }
            } else {
                result.setWillCreate(result.getWillCreate() + 1);
                if (!dryRun) {
                    InventoryItem item = new InventoryItem();
                    applyItem(item, row, idx, code);
                    item.setStatus(ItemStatus.ACTIVE);
                    item.setCurrentStock(0.0);
                    itemRepository.save(item);
                    result.setCreated(result.getCreated() + 1);
                }
            }
        }
    }

    private void applyItem(InventoryItem item, String[] row, Map<String, Integer> idx, String sku) {
        item.setSku(sku);
        item.setItemName(cell(row, idx, "itemName"));
        String cat = cell(row, idx, "category");
        if (cat != null && !cat.isBlank()) {
            item.setCategory(ItemCategory.valueOf(cat.trim().toUpperCase(Locale.ROOT)));
        }
        String unit = cell(row, idx, "unit");
        if (unit != null && !unit.isBlank()) {
            item.setUnit(ItemUnit.valueOf(unit.trim().toUpperCase(Locale.ROOT)));
        }
        String rp = cell(row, idx, "reorderPoint");
        if (rp != null && !rp.isBlank()) {
            item.setReorderPoint(Double.parseDouble(rp.trim()));
        }
    }

    private Map<String, Integer> headerIndex(String[] header) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                idx.put(header[i].trim(), i);
            }
        }
        return idx;
    }

    private String cell(String[] row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i < 0 || i >= row.length) return null;
        String v = row[i];
        return v == null ? null : v.trim();
    }

    /** Simple CSV split — commas only; no quoted-field support required. */
    List<String[]> parseCsv(String csv) {
        List<String[]> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            out.add(line.split(",", -1));
        }
        return out;
    }
}
