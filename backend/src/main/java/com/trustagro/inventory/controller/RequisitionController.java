package com.trustagro.inventory.controller;

import com.trustagro.inventory.entity.InternalRequisition;
import com.trustagro.inventory.service.RequisitionService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/requisitions")
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final UserRepository userRepository;

    public RequisitionController(RequisitionService requisitionService, UserRepository userRepository) {
        this.requisitionService = requisitionService;
        this.userRepository = userRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('inventory:read','inventory:transfer','finance:read','farm:update','veterinary:create','pharmacy:sale')")
    public ResponseEntity<List<InternalRequisition>> getAllRequisitions() {
        return ResponseEntity.ok(requisitionService.getAllRequisitions());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('inventory:transfer','farm:update','veterinary:create','pharmacy:sale')")
    public ResponseEntity<InternalRequisition> createRequisition(@RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(requisitionService.createRequisition(data, getUser(auth)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public ResponseEntity<InternalRequisition> approveRequisition(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(requisitionService.approveRequisition(id, getUser(auth)));
    }

    @PutMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('inventory:stock_out')")
    public ResponseEntity<InternalRequisition> issueRequisition(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(requisitionService.issueRequisition(id, getUser(auth)));
    }
}
