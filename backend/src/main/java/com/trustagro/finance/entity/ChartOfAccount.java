package com.trustagro.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "chart_of_accounts")
@Data
public class ChartOfAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountCode;

    @Column(nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    private BusinessUnit businessUnit;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ChartOfAccount parentAccount;

    private boolean isHeader = false;

    private String description;

    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public void setBusinessUnit(BusinessUnit businessUnit) { this.businessUnit = businessUnit; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setParentAccount(ChartOfAccount parentAccount) { this.parentAccount = parentAccount; }
    public void setHeader(boolean header) { isHeader = header; }
    public void setDescription(String description) { this.description = description; }

    public String getAccountCode() { return accountCode; }
    public String getAccountName() { return accountName; }
    public AccountType getAccountType() { return accountType; }
    public BusinessUnit getBusinessUnit() { return businessUnit; }
    public BigDecimal getBalance() { return balance; }
    public ChartOfAccount getParentAccount() { return parentAccount; }
    public boolean isHeader() { return isHeader; }
    public String getDescription() { return description; }

}
