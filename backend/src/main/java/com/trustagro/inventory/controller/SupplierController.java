package com.trustagro.inventory.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<Supplier>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(supplierService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<Supplier>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventory:create')")
    public ResponseEntity<ApiResponse<Supplier>> create(@RequestBody Supplier supplier) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.create(supplier)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:update')")
    public ResponseEntity<ApiResponse<Supplier>> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        return ResponseEntity.ok(ApiResponse.success(supplierService.update(id, supplier)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:update')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
