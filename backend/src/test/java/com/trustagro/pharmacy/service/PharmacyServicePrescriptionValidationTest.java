package com.trustagro.pharmacy.service;

import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.pharmacy.dto.PosSaleRequest;
import com.trustagro.pharmacy.dto.SaleItemDto;
import com.trustagro.pharmacy.entity.PharmacyPatient;
import com.trustagro.pharmacy.entity.PharmacyPrescription;
import com.trustagro.pharmacy.entity.PrescriptionItem;
import com.trustagro.pharmacy.repository.*;
import com.trustagro.user.entity.User;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacyServicePrescriptionValidationTest {

    @Mock PharmacyPatientRepository patientRepository;
    @Mock PharmacyPrescriptionRepository prescriptionRepository;
    @Mock PrescriptionItemRepository prescriptionItemRepository;
    @Mock PharmacySaleRepository saleRepository;
    @Mock DurAlertRepository durAlertRepository;
    @Mock PharmacyLabelRepository labelRepository;
    @Mock PharmacyCustomerRepository customerRepository;
    @Mock PharmacySaleItemRepository saleItemRepository;
    @Mock com.trustagro.veterinary.repository.PrescriptionRepository vetPrescriptionRepository;
    @Mock com.trustagro.inventory.repository.InventoryItemRepository inventoryItemRepository;
    @Mock com.trustagro.inventory.repository.StockBatchRepository stockBatchRepository;
    @Mock com.trustagro.inventory.repository.StockMovementRepository stockMovementRepository;
    @Mock FinanceService financeService;
    @Mock InventoryService inventoryService;
    @Mock NotificationService notificationService;
    @Mock InvoiceRepository invoiceRepository;

    private PharmacyService service;
    private InventoryItem medicine;
    private PharmacyPatient patient;
    private PharmacyPrescription prescription;
    private PrescriptionItem prescribedItem;

    @BeforeEach
    void setUp() {
        service = new PharmacyService(patientRepository, prescriptionRepository, prescriptionItemRepository,
                saleRepository, durAlertRepository, labelRepository, customerRepository, saleItemRepository,
                vetPrescriptionRepository, inventoryItemRepository, stockBatchRepository, stockMovementRepository,
                financeService, inventoryService, notificationService, invoiceRepository);

        medicine = new InventoryItem();
        medicine.setId(40L);
        medicine.setItemName("Controlled medicine");
        medicine.setIsControlled(true);
        medicine.setRetailPrice(new BigDecimal("25.00"));
        lenient().when(inventoryItemRepository.findById(40L)).thenReturn(Optional.of(medicine));

        patient = new PharmacyPatient();
        patient.setPatientName("Flock patient");
        patient.setIsActive(true);
        setId(patient, 9L);

        prescription = new PharmacyPrescription();
        prescription.setPrescriptionCode("RX-100");
        prescription.setPatient(patient);
        prescription.setStatus("Approved");
        setId(prescription, 100L);

        prescribedItem = new PrescriptionItem();
        prescribedItem.setInventoryItem(medicine);
        prescribedItem.setQuantityPrescribed(new BigDecimal("5"));
        prescribedItem.setQuantityDispensed(BigDecimal.ONE);
        prescribedItem.setIsActive(true);

        lenient().when(prescriptionRepository.findByPrescriptionCodeForUpdate("RX-100"))
                .thenReturn(Optional.of(prescription));
        lenient().when(prescriptionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(prescription));
        lenient().when(prescriptionItemRepository.findByPrescriptionId(100L))
                .thenReturn(List.of(prescribedItem));
        lenient().when(inventoryService.stockOut(anyLong(), anyDouble(), anyString(), any(), any()))
                .thenReturn(Map.of("totalCost", new BigDecimal("4.00")));
        lenient().when(saleRepository.save(any())).thenAnswer(invocation -> {
            var sale = invocation.getArgument(0, com.trustagro.pharmacy.entity.PharmacySale.class);
            if (sale.getId() == null) setId(sale, 101L);
            return sale;
        });
        lenient().when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void controlledSaleResolvesApprovedPrescriptionAndRecordsPartialDispense() {
        PosSaleRequest request = request("RX-100", 2);

        var sale = service.processSale(request, new User());

        assertSame(prescription, sale.getPrescription());
        assertSame(patient, sale.getPatient());
        assertEquals(new BigDecimal("3"), prescribedItem.getQuantityDispensed());
        assertEquals("Approved", prescription.getStatus());
        verify(prescriptionRepository).findByPrescriptionCodeForUpdate("RX-100");
        verify(inventoryService).stockOut(eq(40L), eq(2.0), eq("SALE"), isNull(), any());
    }

    @Test
    void fullyDispensedPrescriptionCannotBeReused() {
        PosSaleRequest first = request("RX-100", 4);
        service.processSale(first, new User());

        assertEquals("Dispensed", prescription.getStatus());
        assertEquals(new BigDecimal("5"), prescribedItem.getQuantityDispensed());
        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("RX-100", 1), new User()));
        verify(inventoryService, times(1)).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void rejectsUnapprovedPrescriptionBeforeStockOut() {
        prescription.setStatus("Pending");

        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("RX-100", 1), new User()));

        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void rejectsMedicineNotOnPrescriptionBeforeStockOut() {
        InventoryItem other = new InventoryItem();
        other.setId(41L);
        other.setItemName("Other medicine");
        other.setIsControlled(true);
        other.setRetailPrice(new BigDecimal("20.00"));
        when(inventoryItemRepository.findById(41L)).thenReturn(Optional.of(other));
        PosSaleRequest request = request("RX-100", 1);
        request.getItems().get(0).setItemId(41L);

        assertThrows(IllegalStateException.class, () -> service.processSale(request, new User()));

        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void rejectsQuantityAboveRemainingPrescriptionAllowance() {
        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("RX-100", 5), new User()));

        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void rejectsMissingPrescriptionAndInactivePatientForControlledMedicine() {
        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("UNKNOWN", 1), new User()));
        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());

        patient.setIsActive(false);
        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("RX-100", 1), new User()));
        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void canonicalVeterinaryPrescriptionCodeIsValidatedAndCannotBeReusedAfterFullDispense() {
        Prescription vetPrescription = new Prescription();
        vetPrescription.setPrescriptionNumber("VET-200");
        vetPrescription.setInventoryItem(medicine);
        vetPrescription.setQuantity(5.0);
        vetPrescription.setFlockId(77L);
        vetPrescription.setStatus(PrescriptionStatus.APPROVED);
        vetPrescription.setCreatedByVet(new User());
        setId(vetPrescription, 200L);
        when(vetPrescriptionRepository.findByPrescriptionNumberForUpdate("VET-200"))
                .thenReturn(Optional.of(vetPrescription));
        when(vetPrescriptionRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(vetPrescription));
        when(patientRepository.findByFlockId(77L)).thenReturn(Optional.of(patient));

        AtomicReference<PharmacyPrescription> savedPrescription = new AtomicReference<>();
        List<PrescriptionItem> generatedItems = new ArrayList<>();
        when(prescriptionRepository.findByPrescriptionCodeForUpdate("VET-200"))
                .thenAnswer(invocation -> Optional.ofNullable(savedPrescription.get()));
        when(prescriptionRepository.findByVetPrescriptionIdForUpdate(200L))
                .thenAnswer(invocation -> Optional.ofNullable(savedPrescription.get()));
        when(prescriptionItemRepository.findByPrescriptionId(101L))
                .thenAnswer(invocation -> generatedItems);
        when(prescriptionItemRepository.save(any())).thenAnswer(invocation -> {
            PrescriptionItem item = invocation.getArgument(0);
            generatedItems.add(item);
            return item;
        });
        when(prescriptionRepository.save(any())).thenAnswer(invocation -> {
            PharmacyPrescription saved = invocation.getArgument(0);
            if (saved.getId() == null) setId(saved, 101L);
            savedPrescription.set(saved);
            return saved;
        });

        var partial = service.processSale(request("VET-200", 2), new User());
        assertSame(patient, partial.getPatient());
        assertEquals(PrescriptionStatus.APPROVED, vetPrescription.getStatus());
        assertEquals(new BigDecimal("2"), generatedItems.get(0).getQuantityDispensed());

        service.processSale(request("VET-200", 3), new User());
        assertEquals("Dispensed", savedPrescription.get().getStatus());
        assertEquals(PrescriptionStatus.COMPLETED, vetPrescription.getStatus());

        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("VET-200", 1), new User()));
        verify(inventoryService, times(2)).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
    }

    @Test
    void canonicalVeterinaryPrescriptionMustBeApprovedBeforeControlledSale() {
        Prescription vetPrescription = new Prescription();
        vetPrescription.setPrescriptionNumber("VET-PENDING");
        vetPrescription.setStatus(PrescriptionStatus.PENDING);
        setId(vetPrescription, 201L);
        when(vetPrescriptionRepository.findByPrescriptionNumberForUpdate("VET-PENDING"))
                .thenReturn(Optional.of(vetPrescription));

        assertThrows(IllegalStateException.class,
                () -> service.processSale(request("VET-PENDING", 1), new User()));

        verify(inventoryService, never()).stockOut(anyLong(), anyDouble(), anyString(), any(), any());
        verify(prescriptionRepository, never()).save(any());
    }

    private PosSaleRequest request(String prescriptionCode, int quantity) {
        SaleItemDto line = new SaleItemDto();
        line.setItemId(40L);
        line.setQty(BigDecimal.valueOf(quantity));
        line.setUnitPrice(new BigDecimal("25.00"));

        PosSaleRequest request = new PosSaleRequest();
        request.setItems(List.of(line));
        request.setPaymentMethod("CASH");
        request.setPrescriptionNumber(prescriptionCode);
        return request;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
