package com.trustagro.farm.service;

import com.trustagro.farm.entity.GrowthRecord;
import com.trustagro.farm.repository.GrowthRecordRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlockManagementService {

    private final GrowthRecordRepository growthRepository;
    private final NotificationService notificationService;

    public void addGrowthRecord(GrowthRecord record) {
        if (record.getBreedStandardWeight() != null && record.getAverageWeight() != null) {
            double deviation = Math.abs(record.getAverageWeight() - record.getBreedStandardWeight()) / record.getBreedStandardWeight();
            if (deviation > 0.1) {
                notificationService.createRoleNotification(
                    "Weight Deviation Alert",
                    "Flock " + record.getFlock().getBatchCode() + " weight deviates by " + (int)(deviation * 100) + "% from standard.",
                    Role.FARM_MANAGER
                );
            }
        }
        growthRepository.save(record);
    }

    /**
     * FCR is not implemented from real feed/weight aggregates yet.
     * @return null — callers must not treat this as a live production figure.
     */
    public Double calculateFCR(Long flockId) {
        return null;
    }

    public List<GrowthRecord> getGrowthHistory(Long flockId) {
        return growthRepository.findByFlockIdOrderBySamplingDateDesc(flockId);
    }
}
