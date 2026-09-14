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
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<FeedRecipe>>> getAllRecipes() {
        return ResponseEntity.ok(ApiResponse.success(feedService.getAllRecipes()));
    }

    @PostMapping("/recipes")
    @PreAuthorize("hasAuthority('farm:create')")
    public ResponseEntity<ApiResponse<FeedRecipe>> createRecipe(@RequestBody FeedRecipe recipe) {
        return ResponseEntity.ok(ApiResponse.success("Recipe created", feedService.createRecipe(recipe)));
    }

    @GetMapping("/silos")
    @PreAuthorize("hasAnyAuthority('farm:read','inventory:read')")
    public ResponseEntity<ApiResponse<List<Silo>>> getAllSilos() {
        return ResponseEntity.ok(ApiResponse.success(feedService.getAllSilos()));
    }

    @PostMapping("/deliveries")
    @PreAuthorize("hasAnyAuthority('farm:update','inventory:stock_in')")
    public ResponseEntity<ApiResponse<Void>> logDelivery(@RequestBody FeedDelivery delivery) {
        feedService.logDelivery(delivery);
        return ResponseEntity.ok(ApiResponse.success("Delivery logged", null));
    }
}
