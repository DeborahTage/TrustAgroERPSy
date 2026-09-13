package com.trustagro.finance.service;

import com.trustagro.finance.entity.CashReconciliation;
import com.trustagro.finance.repository.CashReconciliationRepository;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final CashReconciliationRepository reconciliationRepository;
    private final UserRepository userRepository;
    private final FinanceService financeService;

    @Transactional
    public CashReconciliation performReconciliation(BigDecimal systemAmount, BigDecimal countedAmount, String notes) {
        CashReconciliation rec = new CashReconciliation();
        rec.setExpectedCash(systemAmount);
        rec.setActualCash(countedAmount);
        BigDecimal difference = countedAmount.subtract(systemAmount);
        rec.setVariance(difference);
        rec.setNotes(notes);
        rec.setReconciliationDate(LocalDate.now());
        rec.setStatus(difference.compareTo(BigDecimal.ZERO) == 0 ? "BALANCED"
                : difference.compareTo(BigDecimal.ZERO) > 0 ? "OVER" : "SHORT");

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(rec::setClosedBy);

        rec = reconciliationRepository.save(rec);

        // If there's a shortage or overage, log it to finance
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            String desc = (difference.compareTo(BigDecimal.ZERO) > 0 ? "Overage" : "Shortage") + " in cash reconciliation";
            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                // Shortage: Debit Expense (Misc), Credit Cash
                financeService.postTransaction(com.trustagro.finance.entity.TransactionType.EXPENSE, 
                        difference.abs(), null, "5950", "1000", desc, "RECONCILIATION", "CASH_REC", rec.getId());
            } else {
                // Overage: Debit Cash, Credit Revenue (Misc)
                financeService.postTransaction(com.trustagro.finance.entity.TransactionType.INCOME, 
                        difference, null, "1000", "4900", desc, "RECONCILIATION", "CASH_REC", rec.getId());
            }
        }

        return rec;
    }

    public List<CashReconciliation> getAllReconciliations() {
        return reconciliationRepository.findAll();
    }
}
