package com.trustagro.notification.service;

import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import com.trustagro.veterinary.repository.VaccinationScheduleRepository;
import com.trustagro.user.entity.Role;

@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final VaccinationScheduleRepository vaccinationScheduleRepository;

    public NotificationScheduler(NotificationService notificationService,
                                 InventoryService inventoryService,
                                 VaccinationScheduleRepository vaccinationScheduleRepository) {
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.vaccinationScheduleRepository = vaccinationScheduleRepository;
    }

    /**
     * Run daily at 8:00 AM server time
     * Checks for expiring stock and low stock, and dispatches notifications
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void runDailyInventoryChecks() {
        log.info("Running daily inventory health checks for notifications");

        // Low stock alerts
        inventoryService.getLowStockItems().forEach(item -> {
            notificationService.createLowStockAlert(item.getItemName(), item.getCurrentStock(), item.getUnit().name());
        });

        // Expiry alerts
        inventoryService.getExpiringItems().forEach(batch -> {
            LocalDate expiry = batch.getExpiryDate();
            if (expiry != null) {
                notificationService.createExpiryAlert(
                    batch.getItem().getItemName() + " (Batch " + batch.getBatchNumber() + ")", 
                    expiry.toString()
                );
            }
        });
    }

    /**
     * Run daily at 9:00 AM server time
     * Checks for upcoming vaccinations 3 days from now
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void runDailyVaccinationChecks() {
        log.info("Running daily vaccination schedule checks");
        LocalDate in3Days = LocalDate.now().plusDays(3);
        vaccinationScheduleRepository.findByScheduledDateBetweenAndStatus(
                in3Days, in3Days, com.trustagro.veterinary.entity.VaccinationStatus.SCHEDULED)
            .forEach(v -> {
                notificationService.createRoleNotification(
                    "Upcoming Vaccination Alert",
                    v.getVaccineName() + " for Flock " + v.getFlock().getBatchCode() + " is due on " + v.getScheduledDate(),
                    Role.VETERINARIAN
                );
            });
    }
}
