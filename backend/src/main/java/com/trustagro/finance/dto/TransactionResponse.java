package com.trustagro.finance.dto;

import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private TransactionType transactionType;
    private String category;
    private BigDecimal amount;
    private String debitAccountCode;
    private String creditAccountCode;
    private String sourceModule;
    private String referenceType;
    private Long referenceId;
    private BusinessUnit businessUnit;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;

    public void setId(Long id) { this.id = id; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDebitAccountCode(String debitAccountCode) { this.debitAccountCode = debitAccountCode; }
    public void setCreditAccountCode(String creditAccountCode) { this.creditAccountCode = creditAccountCode; }
    public void setSourceModule(String sourceModule) { this.sourceModule = sourceModule; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setBusinessUnit(BusinessUnit businessUnit) { this.businessUnit = businessUnit; }
    public void setDescription(String description) { this.description = description; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
