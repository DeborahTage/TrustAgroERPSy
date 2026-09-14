package com.trustagro.veterinary.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.repository.FlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final FlockRepository flockRepository;

    public boolean isFlockLocked(Long flockId) {
        Flock flock = flockRepository.findById(flockId).orElseThrow();
        
        // Auto-release if period passed
        if (flock.getIsUnderWithdrawal() && flock.getWithdrawalHoldUntil() != null) {
            if (LocalDate.now().isAfter(flock.getWithdrawalHoldUntil())) {
                flock.setIsUnderWithdrawal(false);
                flockRepository.save(flock);
                return false;
            }
            return true;
        }
        return false;
    }

    public void validateSalesEligibility(Long flockId) {
        if (isFlockLocked(flockId)) {
            Flock flock = flockRepository.findById(flockId).orElseThrow();
            throw new BusinessException("CRITICAL: Biological Lock. Flock " + flock.getBatchCode() + 
                " is under drug withdrawal until " + flock.getWithdrawalHoldUntil() +
                ". Chicken sales are prohibited until the hold ends.");
        }
    }
}
