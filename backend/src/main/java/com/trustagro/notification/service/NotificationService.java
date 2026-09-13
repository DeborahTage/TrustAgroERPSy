package com.trustagro.notification.service;

import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.notification.dto.NotificationResponse;
import com.trustagro.notification.entity.Notification;
import com.trustagro.notification.entity.NotificationType;
import com.trustagro.notification.repository.NotificationRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void createMortalityAlert(String farmName, String batchCode, double rate) {
        create("High Mortality Alert",
                String.format("Farm: %s, Batch: %s - Mortality rate %.1f%% exceeds threshold", farmName, batchCode, rate),
                NotificationType.URGENT, Role.VETERINARIAN, "FARM", null);
        create("High Mortality Alert",
                String.format("Farm: %s, Batch: %s - Mortality rate %.1f%% exceeds threshold", farmName, batchCode, rate),
                NotificationType.URGENT, Role.ADMIN, "FARM", null);
    }

    public void createLowStockAlert(String itemName, double currentStock, String unit) {
        create("Low Stock Alert",
                String.format("Item: %s - Current stock %.1f %s is below minimum level", itemName, currentStock, unit),
                NotificationType.WARNING, Role.INVENTORY_MANAGER, "INVENTORY", null);
    }

    public void createReorderAlert(String itemName, double currentStock, double reorderPoint, String unit) {
        create("Reorder Suggestion",
                String.format("Item: %s - Current stock %.1f %s hit reorder point (%.1f). Draft PR suggested.", itemName, currentStock, unit, reorderPoint),
                NotificationType.INFO, Role.INVENTORY_MANAGER, "INVENTORY", null);
    }

    public void createExpiryAlert(String itemName, String expiryDate) {
        create("Expiry Alert",
                String.format("Item: %s expires on %s (within 30 days)", itemName, expiryDate),
                NotificationType.WARNING, Role.INVENTORY_MANAGER, "INVENTORY", null);
    }

    public void createMissedVaccinationAlert(String farmName, String vaccineName) {
        create("Missed Vaccination",
                String.format("Farm: %s - Vaccination '%s' was missed", farmName, vaccineName),
                NotificationType.WARNING, Role.VETERINARIAN, "VETERINARY", null);
    }

    public void createFollowUpAlert(String clientName, Long clientId) {
        create("CRM Follow-up Due",
                String.format("Follow-up due for client: %s", clientName),
                NotificationType.INFO, Role.CRM_MANAGER, "CRM", clientId);
    }

    public void createRoleNotification(String title, String message, Role role) {
        create(title, message, NotificationType.INFO, role, null, null);
    }

    private void create(String title, String message, NotificationType type, Role role, String module, Long relatedId) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setTargetRole(role);
        n.setRelatedModule(module);
        n.setRelatedId(relatedId);
        notificationRepository.save(n);
    }

    public List<NotificationResponse> getAll(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.findByTargetRoleOrTargetUserIdOrderByCreatedAtDesc(user.getRole(), user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse markRead(Long id, String email) {
        User user = getUserByEmail(email);
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        boolean canAccess = (n.getTargetRole() != null && n.getTargetRole() == user.getRole())
                || (n.getTargetUserId() != null && n.getTargetUserId().equals(user.getId()));
        if (!canAccess) {
            throw new ResourceNotFoundException("Notification not found");
        }
        n.setRead(true);
        return toResponse(notificationRepository.save(n));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setType(n.getType());
        r.setTargetRole(n.getTargetRole());
        r.setRelatedModule(n.getRelatedModule());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
