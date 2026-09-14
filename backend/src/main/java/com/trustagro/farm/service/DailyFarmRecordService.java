package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.DailyFarmRecordRequest;
import com.trustagro.farm.dto.DailyFarmRecordResponse;
import com.trustagro.farm.entity.DailyFarmRecord;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Value("${app.alerts.mortality-threshold}")
    private double mortalityThreshold;

    public List<DailyFarmRecordResponse> getAll() {
        return recordRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DailyFarmRecordResponse> getByFarm(Long farmId) {
        return recordRepository.findByFarmId(farmId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DailyFarmRecordResponse> getByFlock(Long flockId) {
        return recordRepository.findByFlockId(flockId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DailyFarmRecordResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public DailyFarmRecordResponse create(DailyFarmRecordRequest req) {
        if (recordRepository.existsByFarmIdAndFlockIdAndDate(req.getFarmId(), req.getFlockId(), req.getDate()))
            throw new BusinessException("Daily record already exists for this farm, flock, and date");

        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = flockRepository.findById(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));

        DailyFarmRecord record = new DailyFarmRecord();
        mapRequest(req, record, farm, flock);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(record::setRecordedBy);

        DailyFarmRecord saved = recordRepository.save(record);

        // Consume feed from silo if provided
        if (req.getSiloId() != null && req.getFeedConsumed() != null) {
            feedService.consumeFeed(req.getSiloId(), req.getFeedConsumed().doubleValue());
        }

        // Update flock current bird count
        int newCount = (flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : flock.getInitialBirdCount())
                - (req.getMortality() != null ? req.getMortality() : 0)
                - (req.getCulledBirds() != null ? req.getCulledBirds() : 0);
        flock.setCurrentBirdCount(Math.max(0, newCount));
        flockRepository.save(flock);

        // Check mortality alert
        if (req.getMortality() != null && req.getOpeningBirdCount() != null && req.getOpeningBirdCount() > 0) {
            double rate = (double) req.getMortality() / req.getOpeningBirdCount() * 100;
            if (rate > mortalityThreshold) {
                notificationService.createMortalityAlert(farm.getFarmName(), flock.getBatchCode(), rate);
            }
        }

        return toResponse(saved);
    }

    public DailyFarmRecordResponse update(Long id, DailyFarmRecordRequest req) {
        DailyFarmRecord record = findById(id);
        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = flockRepository.findById(req.getFlockId())
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));
        mapRequest(req, record, farm, flock);
        return toResponse(recordRepository.save(record));
    }

    private void mapRequest(DailyFarmRecordRequest req, DailyFarmRecord record, Farm farm, Flock flock) {
        record.setDate(req.getDate());
        record.setFarm(farm);
        record.setFlock(flock);
        record.setOpeningBirdCount(req.getOpeningBirdCount());
        record.setMortality(req.getMortality());
        record.setCulledBirds(req.getCulledBirds());
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
        res.setCulledBirds(r.getCulledBirds());
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
        if (r.getMortality() != null && r.getOpeningBirdCount() != null && r.getOpeningBirdCount() > 0)
            res.setMortalityRate((double) r.getMortality() / r.getOpeningBirdCount() * 100);
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        return res;
    }
}
