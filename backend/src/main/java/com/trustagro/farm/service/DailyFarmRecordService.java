package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.*;
import com.trustagro.farm.entity.*;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyFarmRecordService {

    private final DailyFarmRecordRepository recordRepository;
    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.trustagro.feed.service.FeedService feedService;
    private final com.trustagro.veterinary.repository.DiseaseCaseRepository diseaseCaseRepository;
    private final FarmAccessService farmAccessService;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;

    @Value("${app.alerts.mortality-threshold}")
    private double mortalityThreshold;

    public List<DailyFarmRecordResponse> getAll() {
        List<Long> scope = farmAccessService.accessibleFarmIdsOrNull();
        return recordRepository.findAll().stream()
                .filter(r -> scope == null || scope.contains(r.getFarm().getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<DailyFarmRecordResponse> getByFarm(Long farmId) {
        farmAccessService.assertCanAccessFarm(farmId);
        return recordRepository.findByFarmId(farmId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DailyFarmRecordResponse> getByFlock(Long flockId) {
        Flock flock = flockRepository.findById(flockId)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        farmAccessService.assertCanAccessFarm(flock.getFarm().getId());
        return recordRepository.findByFlockId(flockId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DailyFarmRecordResponse getById(Long id) {
        DailyFarmRecord record = findById(id);
        farmAccessService.assertCanAccessFarm(record.getFarm().getId());
        return toResponse(record);
    }

    @Transactional
    public DailyFarmRecordResponse create(DailyFarmRecordRequest req) {
        farmAccessService.assertCanAccessFarm(req.getFarmId());
        if (recordRepository.existsByFarmIdAndFlockIdAndDate(req.getFarmId(), req.getFlockId(), req.getDate())) {
            throw new BusinessException("Daily record already exists for this farm, flock, and date");
        }

        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = flockRepository.findByIdForUpdate(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        if (!flock.getFarm().getId().equals(farm.getId())) {
            throw new BusinessException("Flock does not belong to the selected farm");
        }

        DailyFarmRecord record = new DailyFarmRecord();
        mapRequest(req, record, farm, flock);
        applyLines(req, record);

        int mortalityFromLines = record.getMortalityLines().stream()
                .mapToInt(l -> l.getDeathCount() != null ? l.getDeathCount() : 0)
                .sum();
        int mortality = !record.getMortalityLines().isEmpty()
                ? mortalityFromLines
                : (req.getMortality() != null ? req.getMortality() : 0);
        record.setMortality(mortality);

        int culled = req.getCulledChickens() != null ? req.getCulledChickens() : 0;
        int available = flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : 0;
        if (mortality + culled > available) {
            throw new BusinessException("Mortality + culled exceeds available birds (" + available + ")");
        }

        validateAndAggregateFeed(record);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(record::setRecordedBy);

        DailyFarmRecord saved = recordRepository.save(record);
        stockOutLines(saved);

        if (req.getSiloId() != null && saved.getFeedConsumed() != null) {
            feedService.consumeFeed(req.getSiloId(), saved.getFeedConsumed().doubleValue());
        }

        int newCount = available - mortality - culled;
        flock.setCurrentBirdCount(Math.max(0, newCount));
        flockRepository.save(flock);

        if (mortality > 0 && req.getOpeningBirdCount() != null && req.getOpeningBirdCount() > 0) {
            double rate = (double) mortality / req.getOpeningBirdCount() * 100;
            if (rate > mortalityThreshold) {
                notificationService.createMortalityAlert(farm.getFarmName(), flock.getBatchCode(), rate);
            }
        }

        return toResponse(saved);
    }

    public DailyFarmRecordResponse update(Long id, DailyFarmRecordRequest req) {
        DailyFarmRecord record = findById(id);
        farmAccessService.assertCanAccessFarm(record.getFarm().getId());
        farmAccessService.assertCanAccessFarm(req.getFarmId());
        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = flockRepository.findById(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        mapRequest(req, record, farm, flock);
        return toResponse(recordRepository.save(record));
    }

    private void applyLines(DailyFarmRecordRequest req, DailyFarmRecord record) {
        record.clearFeedLines();
        record.clearMedicationLines();
        record.clearMortalityLines();

        if (req.getFeedLines() != null) {
            for (DailyFeedUsageLineRequest lineReq : req.getFeedLines()) {
                DailyFeedUsageLine line = new DailyFeedUsageLine();
                if (lineReq.getInventoryItemId() != null) {
                    InventoryItem item = inventoryItemRepository.findById(lineReq.getInventoryItemId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
                    line.setInventoryItem(item);
                }
                line.setFeedType(lineReq.getFeedType());
                BigDecimal issued = nvl(lineReq.getIssuedQuantity());
                BigDecimal returned = nvl(lineReq.getReturnedQuantity());
                if (returned.compareTo(issued) > 0) {
                    throw new BusinessException("Feed return cannot exceed issued quantity");
                }
                line.setIssuedQuantity(issued);
                line.setReturnedQuantity(returned);
                line.setConsumedQuantity(issued.subtract(returned));
                line.setUnit(lineReq.getUnit());
                line.setNotes(lineReq.getNotes());
                record.addFeedLine(line);
            }
        }
        if (req.getMedicationLines() != null) {
            for (DailyMedicationLineRequest lineReq : req.getMedicationLines()) {
                DailyMedicationLine line = new DailyMedicationLine();
                if (lineReq.getInventoryItemId() != null) {
                    InventoryItem item = inventoryItemRepository.findById(lineReq.getInventoryItemId())
                            .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
                    line.setInventoryItem(item);
                }
                line.setTreatmentType(lineReq.getTreatmentType() != null ? lineReq.getTreatmentType() : "MEDICATION");
                line.setQuantity(nvl(lineReq.getQuantity()));
                line.setUnit(lineReq.getUnit());
                line.setNotes(lineReq.getNotes());
                line.setInventoryAlreadyPosted(lineReq.isInventoryAlreadyPosted());
                record.addMedicationLine(line);
            }
        }
        if (req.getMortalityLines() != null) {
            for (DailyMortalityLineRequest lineReq : req.getMortalityLines()) {
                DailyMortalityLine line = new DailyMortalityLine();
                line.setDeathCount(lineReq.getDeathCount() != null ? lineReq.getDeathCount() : 0);
                line.setCause(lineReq.getCause());
                line.setNotes(lineReq.getNotes());
                record.addMortalityLine(line);
            }
        }
    }

    private void validateAndAggregateFeed(DailyFarmRecord record) {
        BigDecimal totalConsumed = record.getFeedLines().stream()
                .map(l -> l.getConsumedQuantity() != null ? l.getConsumedQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!record.getFeedLines().isEmpty()) {
            record.setFeedConsumed(totalConsumed);
        }
    }

    private void stockOutLines(DailyFarmRecord record) {
        var user = farmAccessService.currentUser();
        for (DailyFeedUsageLine line : record.getFeedLines()) {
            if (line.getInventoryItem() == null || line.getStockMovementId() != null) continue;
            BigDecimal consumed = line.getConsumedQuantity() != null ? line.getConsumedQuantity() : BigDecimal.ZERO;
            if (consumed.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> result = inventoryService.stockOut(
                    line.getInventoryItem().getId(),
                    consumed.doubleValue(),
                    "DAILY_FEED",
                    record.getId(),
                    user,
                    null,
                    "DAILY_FARM_RECORD",
                    null);
            Object mid = result.get("movementId");
            if (mid instanceof Number) {
                line.setStockMovementId(((Number) mid).longValue());
            } else {
                line.setStockMovementId(record.getId());
            }
        }
        for (DailyMedicationLine line : record.getMedicationLines()) {
            if (line.isInventoryAlreadyPosted() || line.getInventoryItem() == null || line.getStockMovementId() != null) {
                continue;
            }
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> result = inventoryService.stockOut(
                    line.getInventoryItem().getId(),
                    qty.doubleValue(),
                    "DAILY_MEDICATION",
                    record.getId(),
                    user,
                    null,
                    "DAILY_FARM_RECORD",
                    null);
            Object mid = result.get("movementId");
            if (mid instanceof Number) {
                line.setStockMovementId(((Number) mid).longValue());
            } else {
                line.setStockMovementId(record.getId());
            }
        }
    }

    private void mapRequest(DailyFarmRecordRequest req, DailyFarmRecord record, Farm farm, Flock flock) {
        record.setDate(req.getDate());
        record.setFarm(farm);
        record.setFlock(flock);
        record.setOpeningBirdCount(req.getOpeningBirdCount());
        record.setMortality(req.getMortality());
        record.setCulledChickens(req.getCulledChickens());
        record.setFeedConsumed(req.getFeedConsumed());
        record.setWaterConsumed(req.getWaterConsumed());
        record.setAverageWeight(req.getAverageWeight());
        record.setSymptomsOrRemarks(req.getSymptomsOrRemarks());
        record.setMortalityCause(req.getMortalityCause());
        record.setMortalityNotes(req.getMortalityNotes());
        record.setBarnPhotoUrl(req.getBarnPhotoUrl());

        if (req.getDiseaseCaseId() != null) {
            diseaseCaseRepository.findById(req.getDiseaseCaseId()).ifPresent(record::setDiseaseCase);
        }
        if (req.getSiloId() != null) {
            com.trustagro.feed.entity.Silo silo = new com.trustagro.feed.entity.Silo();
            silo.setId(req.getSiloId());
            record.setSilo(silo);
        }
    }

    private DailyFarmRecord findById(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Daily record not found: " + id));
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public DailyFarmRecordResponse toResponse(DailyFarmRecord r) {
        DailyFarmRecordResponse res = new DailyFarmRecordResponse();
        res.setId(r.getId());
        res.setDate(r.getDate());
        res.setFarmId(r.getFarm().getId());
        res.setFarmName(r.getFarm().getFarmName());
        res.setFlockId(r.getFlock().getId());
        res.setBatchCode(r.getFlock().getBatchCode());
        res.setOpeningBirdCount(r.getOpeningBirdCount());
        res.setMortality(r.getMortality());
        res.setCulledChickens(r.getCulledChickens());
        res.setFeedConsumed(r.getFeedConsumed());
        res.setWaterConsumed(r.getWaterConsumed());
        res.setAverageWeight(r.getAverageWeight());
        res.setSymptomsOrRemarks(r.getSymptomsOrRemarks());
        res.setMortalityCause(r.getMortalityCause());
        res.setMortalityNotes(r.getMortalityNotes());
        res.setBarnPhotoUrl(r.getBarnPhotoUrl());
        if (r.getDiseaseCase() != null) res.setDiseaseCaseId(r.getDiseaseCase().getId());
        if (r.getSilo() != null) res.setSiloId(r.getSilo().getId());
        if (r.getRecordedBy() != null) res.setRecordedBy(r.getRecordedBy().getFullName());
        if (r.getMortality() != null && r.getOpeningBirdCount() != null && r.getOpeningBirdCount() > 0) {
            res.setMortalityRate((double) r.getMortality() / r.getOpeningBirdCount() * 100);
        }
        if (r.getFlock().getStartDate() != null) {
            long days = ChronoUnit.DAYS.between(r.getFlock().getStartDate(),
                    r.getDate() != null ? r.getDate() : LocalDate.now());
            res.setAgeDays(days);
            res.setAgeWeeks(days / 7);
        }
        res.setFeedLines(r.getFeedLines().stream().map(this::toFeedResponse).collect(Collectors.toList()));
        res.setMedicationLines(r.getMedicationLines().stream().map(this::toMedResponse).collect(Collectors.toList()));
        res.setMortalityLines(r.getMortalityLines().stream().map(this::toMortResponse).collect(Collectors.toList()));
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        return res;
    }

    private DailyFeedUsageLineResponse toFeedResponse(DailyFeedUsageLine l) {
        DailyFeedUsageLineResponse r = new DailyFeedUsageLineResponse();
        r.setId(l.getId());
        if (l.getInventoryItem() != null) {
            r.setInventoryItemId(l.getInventoryItem().getId());
            r.setInventoryItemName(l.getInventoryItem().getItemName());
        }
        r.setFeedType(l.getFeedType());
        r.setIssuedQuantity(l.getIssuedQuantity());
        r.setReturnedQuantity(l.getReturnedQuantity());
        r.setConsumedQuantity(l.getConsumedQuantity());
        r.setUnit(l.getUnit());
        r.setNotes(l.getNotes());
        r.setStockMovementId(l.getStockMovementId());
        return r;
    }

    private DailyMedicationLineResponse toMedResponse(DailyMedicationLine l) {
        DailyMedicationLineResponse r = new DailyMedicationLineResponse();
        r.setId(l.getId());
        if (l.getInventoryItem() != null) {
            r.setInventoryItemId(l.getInventoryItem().getId());
            r.setInventoryItemName(l.getInventoryItem().getItemName());
        }
        r.setTreatmentType(l.getTreatmentType());
        r.setQuantity(l.getQuantity());
        r.setUnit(l.getUnit());
        r.setNotes(l.getNotes());
        r.setStockMovementId(l.getStockMovementId());
        r.setInventoryAlreadyPosted(l.isInventoryAlreadyPosted());
        return r;
    }

    private DailyMortalityLineResponse toMortResponse(DailyMortalityLine l) {
        DailyMortalityLineResponse r = new DailyMortalityLineResponse();
        r.setId(l.getId());
        r.setDeathCount(l.getDeathCount());
        r.setCause(l.getCause());
        r.setNotes(l.getNotes());
        return r;
    }
}
