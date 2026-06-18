package com.trustagro.pharmacy.service;

import com.trustagro.farm.entity.Flock;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.MovementType;
import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.inventory.entity.StockMovement;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockBatchRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import com.trustagro.pharmacy.dto.PosSaleRequest;
import com.trustagro.pharmacy.dto.SaleItemDto;
import com.trustagro.pharmacy.entity.*;
import com.trustagro.pharmacy.repository.*;
import com.trustagro.user.entity.User;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.trustagro.inventory.service.InventoryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    private final PharmacyPatientRepository patientRepository;
    private final PharmacyPrescriptionRepository rxRepository;
    private final PrescriptionItemRepository rxItemRepository;
    private final PharmacySaleRepository saleRepository;
    private final DurAlertRepository durAlertRepository;
    private final PharmacyLabelRepository labelRepository;
    private final PharmacyCustomerRepository customerRepository;
    private final PharmacySaleItemRepository saleItemRepository;
    
    private final PrescriptionRepository vetRxRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final FinanceService financeService;
    private final InventoryService inventoryService;

    public PharmacyService(PharmacyPatientRepository patientRepository,
                           PharmacyPrescriptionRepository rxRepository,
                           PrescriptionItemRepository rxItemRepository,
                           PharmacySaleRepository saleRepository,
                           DurAlertRepository durAlertRepository,
                           PharmacyLabelRepository labelRepository,
                           PharmacyCustomerRepository customerRepository,
                           PharmacySaleItemRepository saleItemRepository,
                           PrescriptionRepository vetRxRepository,
                           InventoryItemRepository inventoryItemRepository,
                           StockBatchRepository stockBatchRepository,
                           StockMovementRepository stockMovementRepository,
                           FinanceService financeService,
                           InventoryService inventoryService) {
        this.patientRepository = patientRepository;
        this.rxRepository = rxRepository;
        this.rxItemRepository = rxItemRepository;
        this.saleRepository = saleRepository;
        this.durAlertRepository = durAlertRepository;
        this.labelRepository = labelRepository;
        this.customerRepository = customerRepository;
        this.saleItemRepository = saleItemRepository;
        this.vetRxRepository = vetRxRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.financeService = financeService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public PharmacyPrescription receivePrescription(Long vetPrescriptionId, User pharmacyUser) {
        Prescription vetRx = vetRxRepository.findById(vetPrescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Vet Prescription not found"));

        PharmacyPatient patient = patientRepository.findByFlockId(vetRx.getFlockId())
                .orElseGet(() -> {
                    PharmacyPatient newPatient = new PharmacyPatient();
                    newPatient.setPatientName("Flock-" + vetRx.getFlockId());
                    newPatient.setSpecies("Chicken");
                    return patientRepository.save(newPatient);
                });

        PharmacyPrescription rx = new PharmacyPrescription();
        rx.setPrescriptionCode("RX-" + System.currentTimeMillis());
        rx.setVetPrescription(vetRx);
        rx.setPatient(patient);
        rx.setPrescribedBy(vetRx.getCreatedByVet() != null ? vetRx.getCreatedByVet() : pharmacyUser);
        rx.setDiagnosis("Transferred from Vet Module");
        rx.setStatus("Pending");

        rx = rxRepository.save(rx);
        
        if (vetRx.getInventoryItemId() != null) {
            InventoryItem drug = inventoryItemRepository.findById(vetRx.getInventoryItemId()).orElse(null);
            if (drug != null) {
                PrescriptionItem rxItem = new PrescriptionItem();
                rxItem.setPrescription(rx);
                rxItem.setInventoryItem(drug);
                rxItem.setDosage(vetRx.getDosageInstruction());
                rxItem.setFrequency(vetRx.getFrequency());
                rxItem.setDurationDays(vetRx.getDurationDays());
                rxItem.setQuantityPrescribed(BigDecimal.valueOf(vetRx.getQuantity() != null ? vetRx.getQuantity() : 1.0));
                rxItemRepository.save(rxItem);
            }
        }

        runDUR(rx.getId(), patient.getId());
        return rx;
    }

    @Transactional
    public void runDUR(Long pharmacyRxId, Long patientId) {
        PharmacyPrescription rx = rxRepository.findById(pharmacyRxId).orElseThrow();
        List<PrescriptionItem> items = rxItemRepository.findByPrescriptionId(pharmacyRxId);
        List<Object> warnings = new ArrayList<>();
        
        for (PrescriptionItem item : items) {
            InventoryItem drug = item.getInventoryItem();
            List<StockBatch> batches = stockBatchRepository.findAvailableBatchesFEFO(drug.getId(), LocalDate.now());
            if (batches.isEmpty()) {
                DurAlert alert = new DurAlert();
                alert.setPrescription(rx);
                alert.setAlertType("OUT_OF_STOCK");
                alert.setSeverity("Critical");
                alert.setMessage(drug.getItemName() + " is out of stock");
                durAlertRepository.save(alert);
                warnings.add(Map.of("type", "OUT_OF_STOCK", "severity", "Critical", "message", alert.getMessage()));
            }
        }
        rx.setDurWarnings(warnings);
        rxRepository.save(rx);
    }

    @Transactional
    public void approvePrescription(Long rxId, User user) {
        PharmacyPrescription rx = rxRepository.findById(rxId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
        rx.setStatus("Approved");
        rx.setProcessedBy(user);
        rxRepository.save(rx);
        log.info("Prescription {} approved by pharmacist {}", rx.getPrescriptionCode(), user.getEmail());
    }

    @Transactional
    public void dispensePrescription(Long rxId, User user) {
        PharmacyPrescription rx = rxRepository.findById(rxId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));

        if (!"Approved".equalsIgnoreCase(rx.getStatus())) {
            throw new IllegalStateException("Prescription must be Approved before dispensing");
        }

        List<PrescriptionItem> items = rxItemRepository.findByPrescriptionId(rxId);
        BigDecimal totalCost = BigDecimal.ZERO;

        for (PrescriptionItem item : items) {
            InventoryItem drug = item.getInventoryItem();
            double qtyToDeduct = item.getQuantityPrescribed().doubleValue();

            // Deduct from Inventory
            inventoryService.stockOut(
                    drug.getId(),
                    qtyToDeduct,
                    "PHARMACY_DISPENSE: " + rx.getPrescriptionCode(),
                    null,
                    user
            );

            // Calculate cost (internal cost or retail depending on type)
            BigDecimal unitPrice = drug.getRetailPrice() != null ? drug.getRetailPrice() : BigDecimal.ZERO;
            totalCost = totalCost.add(unitPrice.multiply(item.getQuantityPrescribed()));
        }

        rx.setStatus("Dispensed");
        rx.setProcessedBy(user);
        rx.setTotalCost(totalCost);
        rxRepository.save(rx);

        // Update original Vet Prescription
        if (rx.getVetPrescription() != null) {
            Prescription vetRx = rx.getVetPrescription();
            vetRx.setStatus(com.trustagro.veterinary.entity.PrescriptionStatus.COMPLETED);
            vetRxRepository.save(vetRx);

            // Finance Logging
            if (vetRx.getPrescriptionType() == com.trustagro.veterinary.entity.PrescriptionType.INTERNAL_FARM) {
                financeService.postExpense(totalCost, null, "5120", 
                        "Internal Dispense: " + rx.getPrescriptionCode(), "PHARMACY", "PHARMACY_DISPENSE", rx.getId());
            } else {
                financeService.postRevenue(totalCost, null, "4200", 
                        "Client Sale: " + rx.getPrescriptionCode(), "PHARMACY", "PHARMACY_DISPENSE", rx.getId());
            }

        }

        log.info("Prescription {} dispensed and finance/stock updated", rx.getPrescriptionCode());
    }

    @Transactional
    public PharmacySale processSale(PosSaleRequest request, User user) {
        PharmacySale sale = new PharmacySale();
        sale.setSaleCode("SAL-" + System.currentTimeMillis());
        sale.setSoldBy(user);
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setCustomerName(request.getCustomerName());
        sale.setCustomerPhone(request.getCustomerPhone());
        if (request.getPrescriptionNumber() != null && !request.getPrescriptionNumber().isBlank()) {
            sale.setNotes("Rx: " + request.getPrescriptionNumber());
        }

        PharmacyCustomer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null) {
                sale.setCustomer(customer);
                sale.setCustomerName(customer.getName());
                sale.setCustomerPhone(customer.getPhone());
                
                // Set Price Type based on Customer Type
                if (customer.getCustomerType() == CustomerType.WHOLESALE) sale.setPriceType(PriceType.WHOLESALE);
                else if (customer.getCustomerType() == CustomerType.PARTNER_FARM) sale.setPriceType(PriceType.PARTNER);
                else sale.setPriceType(PriceType.RETAIL);
            }
        }

        if (request.getPrescriptionId() != null) {
            PharmacyPrescription rx = rxRepository.findById(request.getPrescriptionId()).orElse(null);
            if (rx != null) {
                sale.setPrescription(rx);
                sale.setPatient(rx.getPatient());
                rx.setStatus("Dispensed");
                rx.setProcessedBy(user);
                rxRepository.save(rx);
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PharmacySaleItem> saleItems = new ArrayList<>();

        for (SaleItemDto itemDto : request.getItems()) {
            InventoryItem item = inventoryItemRepository.findById(itemDto.getItemId()).orElseThrow();
            
            // PRESCRIPTION GUARD
            if (Boolean.TRUE.equals(item.getIsControlled()) && sale.getPrescription() == null) {
                throw new IllegalStateException("Sale blocked: " + item.getItemName() + " requires a prescription.");
            }

            // Deduct Stock
            Map<String, Object> outResult = inventoryService.stockOut(
                itemDto.getItemId(), 
                itemDto.getQty().doubleValue(), 
                "PHARMACY_SALE", 
                null, 
                user
            );

            // Tiered Pricing logic
            BigDecimal unitPrice = item.getRetailPrice();
            if (sale.getPriceType() == PriceType.WHOLESALE && item.getWholesalePrice() != null) {
                unitPrice = item.getWholesalePrice();
            } else if (sale.getPriceType() == PriceType.PARTNER && item.getPartnerPrice() != null) {
                unitPrice = item.getPartnerPrice();
            }

            BigDecimal lineTotal = unitPrice.multiply(itemDto.getQty());
            subtotal = subtotal.add(lineTotal);

            PharmacySaleItem si = new PharmacySaleItem();
            si.setSale(sale);
            si.setInventoryItem(item);
            si.setItemName(item.getItemName());
            si.setQuantity(itemDto.getQty());
            si.setUnitPrice(unitPrice);
            si.setLineTotal(lineTotal);
            si.setPriceType(sale.getPriceType());
            
            // Traceability: attach first batch used if available from inventory result
            if (outResult.containsKey("batchNumber")) {
                si.setBatchNumber((String) outResult.get("batchNumber"));
            }

            saleItems.add(si);
        }

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.15));
        BigDecimal total = subtotal.add(tax);

        sale.setSaleItems(saleItems);
        sale.setSubtotal(subtotal);
        sale.setTaxAmount(tax);
        sale.setTotalAmount(total);

        // Handle Credit Payment
        if ("CREDIT".equalsIgnoreCase(request.getPaymentMethod()) && customer != null) {
            BigDecimal newBalance = customer.getOutstandingBalance().add(total);
            if (customer.getCreditLimit() != null && newBalance.compareTo(customer.getCreditLimit()) > 0) {
                throw new IllegalStateException("Credit limit exceeded for customer: " + customer.getName());
            }
            customer.setOutstandingBalance(newBalance);
            customerRepository.save(customer);
            sale.setPaymentStatus(PaymentStatus.CREDIT);
        }

        PharmacySale savedSale = saleRepository.save(sale);

        // Revenue Log
        financeService.postRevenue(total, request.getBusinessUnit(), "4200", 
                "Sale: " + savedSale.getSaleCode(), "PHARMACY", "PHARMACY_SALE", savedSale.getId());


        return savedSale;
    }

    public Map<String, Object> getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<PharmacySale> todaySales = saleRepository.findSalesBetweenDates(startOfDay, endOfDay);
        List<PharmacySale> monthSales = saleRepository.findSalesBetweenDates(startOfMonth, endOfDay);

        BigDecimal revenueToday = todaySales.stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueMonth = monthSales.stream()
                .map(s -> s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingRx = rxRepository.countByStatus("Pending");

        // Revenue by category from actual sale items
        Map<String, BigDecimal> revenueByCategory = new HashMap<>();
        // Top sellers by quantity
        Map<String, Long> topSellerQty = new HashMap<>();
        Map<String, BigDecimal> topSellerRevenue = new HashMap<>();

        for (PharmacySale sale : monthSales) {
            if (sale.getSaleItems() != null) {
                for (var si : sale.getSaleItems()) {
                    String cat = si.getInventoryItem() != null && si.getInventoryItem().getCategory() != null
                            ? si.getInventoryItem().getCategory().name() : "OTHER";
                    BigDecimal lineTotal = si.getLineTotal() != null ? si.getLineTotal() : BigDecimal.ZERO;
                    revenueByCategory.merge(cat, lineTotal, BigDecimal::add);

                    String itemName = si.getItemName() != null ? si.getItemName() : "Unknown";
                    topSellerQty.merge(itemName, si.getQuantity() != null ? si.getQuantity().longValue() : 0L, Long::sum);
                    topSellerRevenue.merge(itemName, lineTotal, BigDecimal::add);
                }
            }
        }

        // Build top 5 sellers list
        List<Map<String, Object>> topSellers = topSellerQty.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("quantity", e.getValue());
                    item.put("revenue", topSellerRevenue.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());

        // Build category revenue list
        List<Map<String, Object>> categoryRevenue = revenueByCategory.entrySet().stream()
                .map(e -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("category", e.getKey());
                    cat.put("revenue", e.getValue());
                    return cat;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("revenue")).compareTo((BigDecimal) a.get("revenue")))
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue_today", revenueToday);
        stats.put("revenue_month", revenueMonth);
        stats.put("sales_count_today", todaySales.size());
        stats.put("sales_count_month", monthSales.size());
        stats.put("pending_prescriptions", pendingRx);
        stats.put("top_sellers", topSellers);
        stats.put("revenue_by_category", categoryRevenue);

        return stats;
    }

    public List<PharmacySale> getRecentSales() {
        LocalDateTime start = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return saleRepository.findSalesBetweenDates(start, end);
    }

    @Transactional
    public PharmacyCustomer saveCustomer(PharmacyCustomer customer) {
        if (customer.getCustomerCode() == null) {
            customer.setCustomerCode("CUST-" + System.currentTimeMillis());
        }
        return customerRepository.save(customer);
    }

    public List<PharmacyCustomer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional
    public PharmacyLabel generateLabel(Long rxItemId, User user) {
        PrescriptionItem item = rxItemRepository.findById(rxItemId).orElseThrow();
        String content = "TRUST AGRO VETERINARY PHARMACY\n" +
                "Drug: " + item.getInventoryItem().getItemName() + "\n" +
                "Qty: " + item.getQuantityPrescribed() + "\n" +
                "Date: " + LocalDate.now();
                
        PharmacyLabel label = new PharmacyLabel();
        label.setPrescriptionItem(item);
        label.setLabelContent(content);
        label.setPrintedBy(user);
        label.setPrintedAt(LocalDateTime.now());
        return labelRepository.save(label);
    }
}
