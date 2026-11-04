package com.trustagro.feed.service;

import com.trustagro.feed.entity.*;
import com.trustagro.feed.repository.*;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRecipeRepository recipeRepository;
    private final SiloRepository siloRepository;
    private final FeedDeliveryRepository deliveryRepository;
    private final NotificationService notificationService;

    public List<FeedRecipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @Transactional
    public FeedRecipe createRecipe(FeedRecipe recipe) {
        if (recipe.getIngredients() != null) {
            recipe.getIngredients().forEach(i -> i.setRecipe(recipe));
        }
        return recipeRepository.save(recipe);
    }

    public List<Silo> getAllSilos() {
        return siloRepository.findAll();
    }

    @Transactional
    public void logDelivery(FeedDelivery delivery) {
        Silo silo = siloRepository.findByIdForUpdate(delivery.getSilo().getId()).orElseThrow();
        silo.setCurrentLevel(silo.getCurrentLevel() + delivery.getQuantity());
        siloRepository.save(silo);
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void consumeFeed(Long siloId, Double quantity) {
        Silo silo = siloRepository.findByIdForUpdate(siloId).orElseThrow();
        silo.setCurrentLevel(Math.max(0, silo.getCurrentLevel() - quantity));

        notifyIfLow(silo);
        siloRepository.save(silo);
    }

    @Transactional
    public void reconcileConsumption(Long oldSiloId, BigDecimal oldQuantity,
                                     Long newSiloId, BigDecimal newQuantity) {
        BigDecimal oldConsumed = oldQuantity != null ? oldQuantity : BigDecimal.ZERO;
        BigDecimal newConsumed = newQuantity != null ? newQuantity : BigDecimal.ZERO;
        if (oldConsumed.signum() < 0 || newConsumed.signum() < 0) {
            throw new IllegalArgumentException("Feed consumption cannot be negative");
        }

        TreeSet<Long> siloIds = new TreeSet<>();
        if (oldSiloId != null) siloIds.add(oldSiloId);
        if (newSiloId != null) siloIds.add(newSiloId);
        Map<Long, Silo> silos = new java.util.LinkedHashMap<>();
        for (Long siloId : siloIds) {
            Silo silo = siloRepository.findByIdForUpdate(siloId)
                    .orElseThrow(() -> new IllegalArgumentException("Silo not found: " + siloId));
            silos.put(siloId, silo);
        }

        if (oldSiloId != null) {
            Silo oldSilo = silos.get(oldSiloId);
            double restored = oldConsumed.doubleValue();
            if (oldSiloId.equals(newSiloId)) {
                restored -= newConsumed.doubleValue();
            }
            oldSilo.setCurrentLevel(currentLevel(oldSilo) + restored);
            siloRepository.save(oldSilo);
            notifyIfLow(oldSilo);
        }
        if (newSiloId != null && !newSiloId.equals(oldSiloId)) {
            Silo newSilo = silos.get(newSiloId);
            double updatedLevel = currentLevel(newSilo) - newConsumed.doubleValue();
            if (updatedLevel < 0) {
                throw new IllegalStateException("Insufficient feed in silo " + newSilo.getName());
            }
            newSilo.setCurrentLevel(updatedLevel);
            siloRepository.save(newSilo);
            notifyIfLow(newSilo);
        } else if (newSiloId != null) {
            Silo silo = silos.get(newSiloId);
            if (currentLevel(silo) < 0) {
                throw new IllegalStateException("Insufficient feed in silo " + silo.getName());
            }
        }
    }

    private double currentLevel(Silo silo) {
        return silo.getCurrentLevel() != null ? silo.getCurrentLevel() : 0.0;
    }

    private void notifyIfLow(Silo silo) {
        // Alert if level < 20% capacity
        if (silo.getCapacity() != null && silo.getCurrentLevel() != null
                && silo.getCurrentLevel() < (silo.getCapacity() * 0.2)) {
            notificationService.createRoleNotification(
                "Low Silo Alert",
                "Silo " + silo.getName() + " is below 20% capacity (" + silo.getCurrentLevel().intValue() + " kg remaining).",
                Role.FARM_MANAGER
            );
        }
    }
}
