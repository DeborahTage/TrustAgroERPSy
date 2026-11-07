package com.trustagro.farm.service;

import com.trustagro.farm.entity.Barn;
import com.trustagro.farm.repository.BarnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BarnService {

    private final BarnRepository barnRepository;

    public List<Barn> getAllBarns() {
        return barnRepository.findAll();
    }

    public List<Barn> getBarnsByFarm(Long farmId) {
        return barnRepository.findByFarmId(farmId);
    }

    @Transactional
    public Barn createBarn(Barn barn) {
        return barnRepository.save(barn);
    }

    @Transactional
    public Barn updateBarn(Long id, Barn barnDetails) {
        Barn barn = barnRepository.findById(id).orElseThrow();
        barn.setName(barnDetails.getName());
        barn.setCapacity(barnDetails.getCapacity());
        barn.setBarnType(barnDetails.getBarnType());
        barn.setStatus(barnDetails.getStatus());
        return barnRepository.save(barn);
    }
}
