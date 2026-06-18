package com.trustagro.pharmacy.controller;

import com.trustagro.pharmacy.dto.PosSaleRequest;
import com.trustagro.pharmacy.dto.ProcessPrescriptionRequest;
import com.trustagro.pharmacy.entity.DurAlert;
import com.trustagro.pharmacy.entity.PharmacyCustomer;
import com.trustagro.pharmacy.entity.PharmacyPrescription;
import com.trustagro.pharmacy.entity.PharmacySale;
import com.trustagro.pharmacy.repository.DurAlertRepository;
import com.trustagro.pharmacy.repository.PharmacyPrescriptionRepository;
import com.trustagro.pharmacy.repository.PharmacySaleRepository;
import com.trustagro.pharmacy.service.PharmacyService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final PharmacyPrescriptionRepository rxRepository;
    private final PharmacySaleRepository saleRepository;
    private final DurAlertRepository durAlertRepository;
    private final UserRepository userRepository;

    public PharmacyController(PharmacyService pharmacyService,
                              PharmacyPrescriptionRepository rxRepository,
                              PharmacySaleRepository saleRepository,
                              DurAlertRepository durAlertRepository,
                              UserRepository userRepository) {
        this.pharmacyService = pharmacyService;
        this.rxRepository = rxRepository;
        this.saleRepository = saleRepository;
        this.durAlertRepository = durAlertRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<List<PharmacyPrescription>> getPrescriptions(@RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(rxRepository.findByStatus(status));
        }
        return ResponseEntity.ok(rxRepository.findAll());
    }

    @PostMapping("/prescriptions/{id}/process")
    public ResponseEntity<?> processPrescription(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        PharmacyPrescription rx = pharmacyService.receivePrescription(id, user);
        return ResponseEntity.ok(Map.of(
            "prescription", rx,
            "durWarnings", rx.getDurWarnings() != null ? rx.getDurWarnings() : List.of()
        ));
    }

    @PostMapping("/prescriptions/{id}/approve")
    public ResponseEntity<?> approvePrescription(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        pharmacyService.approvePrescription(id, user);
        return ResponseEntity.ok(Map.of("message", "Prescription approved"));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    public ResponseEntity<?> dispensePrescription(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        pharmacyService.dispensePrescription(id, user);
        return ResponseEntity.ok(Map.of("message", "Prescription dispensed successfully"));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<PharmacySale>> getSales() {
        return ResponseEntity.ok(pharmacyService.getRecentSales());
    }

    @PostMapping("/sales")
    public ResponseEntity<PharmacySale> createSale(@RequestBody PosSaleRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        PharmacySale sale = pharmacyService.processSale(request, user);
        return ResponseEntity.ok(sale);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(pharmacyService.getDashboardStats());
    }

    @GetMapping("/customers")
    public ResponseEntity<List<PharmacyCustomer>> getCustomers() {
        return ResponseEntity.ok(pharmacyService.getAllCustomers());
    }

    @PostMapping("/customers")
    public ResponseEntity<PharmacyCustomer> createCustomer(@RequestBody PharmacyCustomer customer) {
        return ResponseEntity.ok(pharmacyService.saveCustomer(customer));
    }

    @GetMapping("/dur-alerts")
    public ResponseEntity<List<DurAlert>> getDurAlerts(@RequestParam(required = false) Boolean resolved) {
        if (Boolean.FALSE.equals(resolved)) {
            return ResponseEntity.ok(durAlertRepository.findByResolvedFalse());
        }
        return ResponseEntity.ok(durAlertRepository.findAll());
    }

    @PostMapping("/labels/{rxItemId}")
    public ResponseEntity<?> generateLabel(@PathVariable Long rxItemId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(pharmacyService.generateLabel(rxItemId, user));
    }
}
