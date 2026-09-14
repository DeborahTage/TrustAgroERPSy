package com.trustagro.finance.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.finance.entity.CashReconciliation;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.CashReconciliationRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final CashReconciliationRepository reconciliationRepository;
    private final UserRepository userRepository;
    private final FinanceService financeService;

    /**
     * Expected cash always comes from the ledger cash account (1000).
     * Callers may pass systemAmount for display, but authoritative expected = COA cash balance.
     */
    @Transactional
    public CashReconciliation performReconciliation(BigDecimal systemAmount, BigDecimal countedAmount, String notes) {
        BigDecimal expected = financeService.getCashOnHandBalance();
        if (systemAmount != null && systemAmount.compareTo(expected) != 0) {
            // Prefer ledger; ignore stale UI copy if it disagrees
            expected = financeService.getCashOnHandBalance();
        }
        if (countedAmount == null) {
            throw new BusinessException("Counted cash amount is required");
        }

        CashReconciliation rec = new CashReconciliation();
        rec.setExpectedCash(expected);
        rec.setActualCash(countedAmount);
        BigDecimal difference = countedAmount.subtract(expected);
        rec.setVariance(difference);
        rec.setNotes(notes);
        rec.setReconciliationDate(LocalDate.now());
        rec.setStatus(difference.compareTo(BigDecimal.ZERO) == 0 ? "BALANCED"
                : difference.compareTo(BigDecimal.ZERO) > 0 ? "OVER" : "SHORT");

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(rec::setClosedBy);

        rec = reconciliationRepository.save(rec);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            String desc = (difference.compareTo(BigDecimal.ZERO) > 0 ? "Overage" : "Shortage")
                    + " in cash reconciliation #" + rec.getId();
            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                financeService.postTransaction(TransactionType.EXPENSE,
                        difference.abs(), null, "5950", "1000", desc,
                        FinanceSourceTypes.MODULE_RECONCILIATION, FinanceSourceTypes.REF_CASH_REC, rec.getId());
            } else {
                financeService.postTransaction(TransactionType.INCOME,
                        difference, null, "1000", "4900", desc,
                        FinanceSourceTypes.MODULE_RECONCILIATION, FinanceSourceTypes.REF_CASH_REC, rec.getId());
            }
        }

        return rec;
    }

    public Map<String, Object> getExpectedCashSnapshot() {
        Map<String, Object> snap = new HashMap<>();
        BigDecimal cash = financeService.getCashOnHandBalance();
        snap.put("expectedCash", cash);
        snap.put("cashAccount", "1000");
        snap.put("asOf", LocalDate.now());
        snap.put("note", "Expected cash is the Chart of Accounts cash-on-hand balance from posted finance transactions.");
        return snap;
    }

    public List<CashReconciliation> getAllReconciliations() {
        return reconciliationRepository.findAll();
    }
}
