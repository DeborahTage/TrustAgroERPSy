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
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlockService {

    private final FlockRepository flockRepository;
    private final FarmRepository farmRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final com.trustagro.veterinary.service.VaccinationAutomationService vaccinationAutomationService;
    private final FarmAccessService farmAccessService;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    public List<FlockResponse> getAll() {
        List<Long> scope = farmAccessService.accessibleFarmIdsOrNull();
        List<Flock> flocks = scope == null
                ? flockRepository.findAll()
                : flockRepository.findByFarmIdIn(scope);
        return flocks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlockResponse getById(Long id) {
        Flock flock = findById(id);
        farmAccessService.assertCanAccessFarm(flock.getFarm().getId());
        return toResponse(flock);
    }

    public FlockResponse create(FlockRequest req) {
        farmAccessService.assertCanAccessFarm(req.getFarmId());
        if (req.getInitialBirdCount() == null || req.getInitialBirdCount() <= 0) {
            throw new BusinessException("Initial bird count must be positive");
        }
        if (flockRepository.existsByBatchCodeIgnoreCase(req.getBatchCode())) {
            throw new BusinessException("Batch code already exists: " + req.getBatchCode());
        }
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
        flock.setNotes(req.getNotes());
        flock.setSourceDocumentUrl(req.getSourceDocumentUrl());
        applyOptionalRefs(flock, req);
        Flock savedFlock = flockRepository.save(flock);

        vaccinationAutomationService.generateScheduleForFlock(savedFlock);

        return toResponse(savedFlock);
    }

    public FlockResponse update(Long id, FlockRequest req) {
        Flock flock = findById(id);
        farmAccessService.assertCanAccessFarm(flock.getFarm().getId());
        flock.setBirdType(req.getBirdType());
        flock.setExpectedEndDate(req.getExpectedEndDate());
        flock.setBreed(req.getBreed());
        flock.setNotes(req.getNotes());
        flock.setSourceDocumentUrl(req.getSourceDocumentUrl());
        applyOptionalRefs(flock, req);
        return toResponse(flockRepository.save(flock));
    }

    public FlockResponse close(Long id) {
        Flock flock = findById(id);
        farmAccessService.assertCanAccessFarm(flock.getFarm().getId());
        if (prescriptionRepository.existsByFlockIdAndWithdrawalEndDateAfter(id, LocalDate.now().minusDays(1))) {
            throw new BusinessException("Cannot close/harvest flock: Active drug withdrawal period.");
        }
        flock.setStatus(FlockStatus.CLOSED);
        return toResponse(flockRepository.save(flock));
    }

    private void applyOptionalRefs(Flock flock, FlockRequest req) {
        if (req.getSupplierId() != null) {
            flock.setSupplier(supplierRepository.findById(req.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found")));
        }
        if (req.getResponsiblePersonId() != null) {
            flock.setResponsiblePerson(userRepository.findById(req.getResponsiblePersonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsible user not found")));
        }
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
        r.setBreed(flock.getBreed());
        r.setInitialBirdCount(flock.getInitialBirdCount());
        r.setCurrentBirdCount(flock.getCurrentBirdCount());
        r.setStartDate(flock.getStartDate());
        r.setExpectedEndDate(flock.getExpectedEndDate());
        r.setStatus(flock.getStatus());
        if (flock.getSupplier() != null) {
            r.setSupplierId(flock.getSupplier().getId());
            r.setSupplierName(flock.getSupplier().getName());
        }
        if (flock.getResponsiblePerson() != null) {
            r.setResponsiblePersonId(flock.getResponsiblePerson().getId());
            r.setResponsiblePersonName(flock.getResponsiblePerson().getFullName());
        }
        r.setNotes(flock.getNotes());
        r.setSourceDocumentUrl(flock.getSourceDocumentUrl());
        if (flock.getStartDate() != null) {
            long days = ChronoUnit.DAYS.between(flock.getStartDate(), LocalDate.now());
            r.setAgeDays(days);
            r.setAgeWeeks(days / 7);
        }
        r.setCreatedAt(flock.getCreatedAt());
        r.setUpdatedAt(flock.getUpdatedAt());
        return r;
    }
}
