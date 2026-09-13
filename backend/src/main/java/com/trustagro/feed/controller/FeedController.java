package com.trustagro.feed.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.feed.entity.*;
import com.trustagro.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<List<FeedRecipe>>> getAllRecipes() {
        return ResponseEntity.ok(ApiResponse.success(feedService.getAllRecipes()));
    }

    @PostMapping("/recipes")
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    public ResponseEntity<ApiResponse<FeedRecipe>> createRecipe(@RequestBody FeedRecipe recipe) {
        return ResponseEntity.ok(ApiResponse.success("Recipe created", feedService.createRecipe(recipe)));
    }

    @GetMapping("/silos")
    public ResponseEntity<ApiResponse<List<Silo>>> getAllSilos() {
        return ResponseEntity.ok(ApiResponse.success(feedService.getAllSilos()));
    }

    @PostMapping("/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER','INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> logDelivery(@RequestBody FeedDelivery delivery) {
        feedService.logDelivery(delivery);
        return ResponseEntity.ok(ApiResponse.success("Delivery logged", null));
    }
}
