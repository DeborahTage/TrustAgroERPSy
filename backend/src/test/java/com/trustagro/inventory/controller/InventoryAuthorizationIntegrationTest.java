package com.trustagro.inventory.controller;

import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.StockMovementRepository;
import com.trustagro.pharmacy.repository.PharmacyPrescriptionRepository;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PharmacyPrescriptionRepository prescriptionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private FinanceTransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void pharmacistCannotCreateOrEditInventoryOrCreateFinanceInvoice() throws Exception {
        String token = loginAs(Role.PHARMACIST);
        InventoryItem item = new InventoryItem();
        item.setSku("SECURITY-" + UUID.randomUUID());
        item.setItemName("Controlled medicine");
        item.setCurrentStock(12.0);
        item = inventoryItemRepository.saveAndFlush(item);

        long itemCount = inventoryItemRepository.count();
        long movementCount = stockMovementRepository.count();
        long prescriptionCount = prescriptionRepository.count();
        long invoiceCount = invoiceRepository.count();
        long transactionCount = transactionRepository.count();

        mockMvc.perform(post("/api/inventory/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"FORBIDDEN-%s","itemName":"Unauthorized item","currentStock":100}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/inventory/items/{id}", item.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"Tampered","currentStock":999}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/inventory/pharmacy-deduct")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"quantity":2}
                                """.formatted(item.getId())))
                        .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/finance/invoices/pos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientName":"Walk-in","unit":"GENERAL","subtotal":1,"vatAmount":0}
                                """))
                .andExpect(status().isForbidden());

        assertEquals(12.0, inventoryItemRepository.findById(item.getId()).orElseThrow().getCurrentStock());
        assertEquals(itemCount, inventoryItemRepository.count());
        assertEquals(movementCount, stockMovementRepository.count());
        assertEquals(prescriptionCount, prescriptionRepository.count());
        assertEquals(invoiceCount, invoiceRepository.count());
        assertEquals(transactionCount, transactionRepository.count());
    }

    @Test
    void inventoryRoleCannotSetStockThroughItemCreateOrEdit() throws Exception {
        String token = loginAs(Role.INVENTORY_MANAGER);
        InventoryItem item = new InventoryItem();
        item.setSku("ADJUSTMENT-" + UUID.randomUUID());
        item.setItemName("Tracked stock");
        item.setCurrentStock(7.0);
        item = inventoryItemRepository.saveAndFlush(item);

        mockMvc.perform(put("/api/inventory/items/{id}", item.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentStock":999}
                                """))
                .andExpect(status().isOk());

        assertEquals(7.0, inventoryItemRepository.findById(item.getId()).orElseThrow().getCurrentStock());

        mockMvc.perform(post("/api/inventory/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"ZERO-STOCK-%s","itemName":"New item","currentStock":999}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.currentStock").value(0.0));
    }

    private String loginAs(Role role) throws Exception {
        User user = new User();
        user.setFullName("Authorization test user");
        user.setEmail("inventory-security-" + UUID.randomUUID() + "@trustagro.test");
        user.setPassword(passwordEncoder.encode("inventory-security-test-password"));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"inventory-security-test-password"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(response).path("data").path("token");
        return token.asText();
    }
}
