package com.trustagro.farm.sale.dto;

import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import com.trustagro.farm.sale.entity.ChickenSaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChickenSaleResponse {
    private Long id;
    private String saleNumber;
    private Long farmId;
    private String farmName;
    private Long barnId;
    private String barnName;
    private Long flockId;
    private String flockBatchCode;
    private Integer flockPopulation;
    private Long customerId;
    private String customerName;
    private LocalDate saleDate;
    private ChickenSaleStatus status;
    private ChickenSaleType saleType;
    private String notes;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private Integer totalChickens;
    private Long invoiceId;
    private String invoiceNumber;
    private String createdByName;
    private String confirmedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChickenSaleItemResponse> items = new ArrayList<>();
    private List<ChickenSalePaymentResponse> payments = new ArrayList<>();
    private List<FlockPopulationEventResponse> populationEvents = new ArrayList<>();
    private BigDecimal revenue;
    private BigDecimal costOfGoodsSold;
    private BigDecimal profit;
    private boolean profitabilityAvailable;
    private String profitabilityNote;
}
