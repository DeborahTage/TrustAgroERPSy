package com.trustagro.farm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.dto.FarmRequest;
import com.trustagro.farm.dto.FarmResponse;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final com.trustagro.farm.repository.BiosecurityProtocolRepository biosecurityRepo;
    private final FarmAccessService farmAccessService;

    public List<com.trustagro.farm.dto.BiosecurityProtocolResponse> getBiosecurityLogs(Long farmId) {
        farmAccessService.assertCanAccessFarm(farmId);
        return biosecurityRepo.findByFarmIdOrderByTimestampDesc(farmId).stream().map(this::toBiosecurityResponse).collect(Collectors.toList());
    }

    public com.trustagro.farm.dto.BiosecurityProtocolResponse logBiosecurity(com.trustagro.farm.dto.BiosecurityProtocolRequest req) {
        farmAccessService.assertCanAccessFarm(req.getFarmId());
        com.trustagro.farm.entity.BiosecurityProtocol bp = new com.trustagro.farm.entity.BiosecurityProtocol();
        bp.setFarm(findById(req.getFarmId()));
        bp.setFootbathRefreshed(req.isFootbathRefreshed());
        bp.setVehicularSpray(req.isVehicularSpray());
        bp.setPpeWorn(req.isPpeWorn());
        bp.setVisitorLogged(req.isVisitorLogged());
        bp.setVehicleReg(req.getVehicleReg());
        bp.setVisitorName(req.getVisitorName());
        bp.setNotes(req.getNotes());
        bp.setStaffSignature(req.getStaffSignature());
        return toBiosecurityResponse(biosecurityRepo.save(bp));
    }

    private com.trustagro.farm.dto.BiosecurityProtocolResponse toBiosecurityResponse(com.trustagro.farm.entity.BiosecurityProtocol bp) {
        com.trustagro.farm.dto.BiosecurityProtocolResponse r = new com.trustagro.farm.dto.BiosecurityProtocolResponse();
        r.setId(bp.getId());
        r.setFarmId(bp.getFarm().getId());
        r.setFarmName(bp.getFarm().getFarmName());
        r.setFootbathRefreshed(bp.isFootbathRefreshed());
        r.setVehicularSpray(bp.isVehicularSpray());
        r.setPpeWorn(bp.isPpeWorn());
        r.setVisitorLogged(bp.isVisitorLogged());
        r.setVehicleReg(bp.getVehicleReg());
        r.setVisitorName(bp.getVisitorName());
        r.setNotes(bp.getNotes());
        r.setStaffSignature(bp.getStaffSignature());
        r.setTimestamp(bp.getTimestamp());
        return r;
    }

    public List<FarmResponse> getAll() {
        List<Long> scope = farmAccessService.accessibleFarmIdsOrNull();
        List<Farm> farms = scope == null
                ? farmRepository.findAll()
                : farmRepository.findByIdIn(scope);
        return farms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FarmResponse getById(Long id) {
        farmAccessService.assertCanAccessFarm(id);
        return toResponse(findById(id));
    }

    public FarmResponse create(FarmRequest req) {
        Farm farm = new Farm();
        mapRequest(req, farm);
        return toResponse(farmRepository.save(farm));
    }

    public FarmResponse update(Long id, FarmRequest req) {
        farmAccessService.assertCanAccessFarm(id);
        Farm farm = findById(id);
        mapRequest(req, farm);
        return toResponse(farmRepository.save(farm));
    }

    public FarmResponse updateStatus(Long id, FarmStatus status) {
        farmAccessService.assertCanAccessFarm(id);
        Farm farm = findById(id);
        farm.setStatus(status);
        return toResponse(farmRepository.save(farm));
    }

    private void mapRequest(FarmRequest req, Farm farm) {
        farm.setFarmName(req.getFarmName());
        farm.setLocation(req.getLocation());
        farm.setFarmType(req.getFarmType());
        farm.setCapacity(req.getCapacity());
        if (req.getAssignedFarmManagerId() != null) {
            User manager = userRepository.findById(req.getAssignedFarmManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            farm.setAssignedFarmManager(manager);
        }
    }

    private Farm findById(Long id) {
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + id));
    }

    public FarmResponse toResponse(Farm farm) {
        FarmResponse r = new FarmResponse();
        r.setId(farm.getId());
        r.setFarmName(farm.getFarmName());
        r.setLocation(farm.getLocation());
        r.setFarmType(farm.getFarmType());
        r.setCapacity(farm.getCapacity());
        r.setStatus(farm.getStatus());
        r.setCreatedAt(farm.getCreatedAt());
        r.setUpdatedAt(farm.getUpdatedAt());
        if (farm.getAssignedFarmManager() != null) {
            r.setAssignedFarmManagerId(farm.getAssignedFarmManager().getId());
            r.setAssignedFarmManagerName(farm.getAssignedFarmManager().getFullName());
        }
        return r;
    }
}
