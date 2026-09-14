package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.FlockRequest;
import com.trustagro.farm.dto.FlockResponse;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.veterinary.entity.VaccinationSchedule;
import com.trustagro.veterinary.entity.VaccinationStatus;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import com.trustagro.veterinary.repository.VaccinationScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlockService {

    private final FlockRepository flockRepository;
    private final FarmRepository farmRepository;
    private final VaccinationScheduleRepository vaccinationScheduleRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final com.trustagro.veterinary.service.VaccinationAutomationService vaccinationAutomationService;

    public List<FlockResponse> getAll() {
        return flockRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlockResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public FlockResponse create(FlockRequest req) {
        Farm farm = farmRepository.findById(req.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        Flock flock = new Flock();
        flock.setBatchCode(req.getBatchCode());
        flock.setFarm(farm);
        flock.setBirdType(req.getBirdType());
        flock.setInitialBirdCount(req.getInitialBirdCount());
        flock.setCurrentBirdCount(req.getInitialBirdCount());
        flock.setStartDate(req.getStartDate());
        flock.setExpectedEndDate(req.getExpectedEndDate());
        flock.setBreed(req.getBreed());
        Flock savedFlock = flockRepository.save(flock);
        
        // Automated Scheduling
        vaccinationAutomationService.generateScheduleForFlock(savedFlock);
        
        return toResponse(savedFlock);
    }

    public FlockResponse update(Long id, FlockRequest req) {
        Flock flock = findById(id);
        flock.setBirdType(req.getBirdType());
        flock.setExpectedEndDate(req.getExpectedEndDate());
        return toResponse(flockRepository.save(flock));
    }

    public FlockResponse close(Long id) {
        Flock flock = findById(id);
        if (prescriptionRepository.existsByFlockIdAndWithdrawalEndDateAfter(id, LocalDate.now().minusDays(1))) {
            throw new BusinessException("Cannot close/harvest flock: Active drug withdrawal period.");
        }
        flock.setStatus(FlockStatus.CLOSED);
        return toResponse(flockRepository.save(flock));
    }

    private Flock findById(Long id) {
        return flockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found: " + id));
    }

    public FlockResponse toResponse(Flock flock) {
        FlockResponse r = new FlockResponse();
        r.setId(flock.getId());
        r.setBatchCode(flock.getBatchCode());
        r.setFarmId(flock.getFarm().getId());
        r.setFarmName(flock.getFarm().getFarmName());
        if (flock.getBarn() != null) {
            r.setBarnId(flock.getBarn().getId());
            r.setBarnName(flock.getBarn().getName());
        }
        r.setBirdType(flock.getBirdType());
        r.setInitialBirdCount(flock.getInitialBirdCount());
        r.setCurrentBirdCount(flock.getCurrentBirdCount());
        r.setStartDate(flock.getStartDate());
        r.setExpectedEndDate(flock.getExpectedEndDate());
        r.setStatus(flock.getStatus());
        r.setCreatedAt(flock.getCreatedAt());
        r.setUpdatedAt(flock.getUpdatedAt());
        return r;
    }
}
