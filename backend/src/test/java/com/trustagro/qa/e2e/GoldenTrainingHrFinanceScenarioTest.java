package com.trustagro.qa.e2e;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.qa.fixture.QaEntities;
import com.trustagro.qa.fixture.QaFinanceHarness;
import com.trustagro.training.dto.TrainingPaymentRequest;
import com.trustagro.training.entity.*;
import com.trustagro.training.repository.*;
import com.trustagro.training.service.TrainingService;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoldenTrainingHrFinanceScenarioTest {

    @Mock TrainingProgramRepository programRepository;
    @Mock TrainingSessionRepository sessionRepository;
    @Mock TrainingRegistrationRepository registrationRepository;
    @Mock TrainingPaymentRepository paymentRepository;
    @Mock TrainingAttendanceRepository attendanceRepository;
    @Mock TrainingCertificateRepository certificateRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    private QaFinanceHarness finance;
    private TrainingService trainingService;
    private final AtomicLong ids = new AtomicLong(1);
    private TrainingRegistration reg;
    private TrainingSession session;

    @BeforeEach
    void setUp() {
        finance = new QaFinanceHarness();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("training@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = QaEntities.user(1L, "training@test.com", "Coordinator", Role.TRAINING_COORDINATOR);
        lenient().when(userRepository.findByEmail("training@test.com")).thenReturn(Optional.of(user));

        CrmClient client = QaEntities.client(5L, "Trainee Co");
        TrainingProgram program = new TrainingProgram();
        QaEntities.setId(program, 1L);
        program.setProgramCode("APFM");
        program.setTitle("Poultry Mgmt");
        program.setPrice(new BigDecimal("10000.00"));
        program.setCurrency("ETB");
        program.setStatus(ProgramStatus.REGISTRATION_OPEN);

        session = new TrainingSession();
        QaEntities.setId(session, 10L);
        session.setProgram(program);
        session.setSessionCode("APFM-1");
        session.setStartDate(LocalDate.now().plusDays(7));
        session.setEndDate(LocalDate.now().plusDays(9));
        session.setCapacity(30);
        session.setConfirmedCount(0);
        session.setStatus(SessionStatus.SCHEDULED);

        reg = new TrainingRegistration();
        QaEntities.setId(reg, 20L);
        reg.setRegistrationNumber("TR-20");
        reg.setSession(session);
        reg.setClient(client);
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setPaymentStatus(TrainingPaymentStatus.UNPAID);
        reg.setTotalAmount(new BigDecimal("10000.00"));
        reg.setPaidAmount(BigDecimal.ZERO.setScale(2));
        reg.setOutstandingAmount(new BigDecimal("10000.00"));
        reg.setPrice(new BigDecimal("10000.00"));

        lenient().when(registrationRepository.findById(20L)).thenReturn(Optional.of(reg));
        lenient().when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        lenient().when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> {
            TrainingPayment p = inv.getArgument(0);
            if (p.getId() == null) QaEntities.setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(finance.invoiceRepository().save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) QaEntities.setId(i, ids.getAndIncrement());
            return i;
        });
        lenient().when(finance.invoiceRepository().findBySourceModuleAndSourceId(any(), any()))
                .thenReturn(Optional.empty());

        trainingService = new TrainingService(
                programRepository, sessionRepository, registrationRepository, paymentRepository,
                attendanceRepository, certificateRepository, clientRepository,
                finance.invoiceRepository(), finance.finance(), userRepository,
                finance.auditService(), notificationService);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void golden_trainingConfirmPostsIncome_paymentIsTransferOnly() {
        trainingService.confirmRegistration(20L);
        assertEquals(RegistrationStatus.CONFIRMED, reg.getStatus());
        assertEquals(1L, finance.countByType(TransactionType.INCOME));
        assertEquals(0, new BigDecimal("10000.00").compareTo(finance.sumByType(TransactionType.INCOME)));
        assertEquals(0, new BigDecimal("10000.00").compareTo(finance.balance(AccountCode.REV_TRAINING.getCode())));

        TrainingPaymentRequest pay = new TrainingPaymentRequest();
        pay.setAmount(new BigDecimal("10000.00"));
        pay.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        trainingService.recordPayment(20L, pay);

        assertEquals(TrainingPaymentStatus.PAID, reg.getPaymentStatus());
        assertEquals(1L, finance.countByType(TransactionType.INCOME));
        assertTrue(finance.countByType(TransactionType.TRANSFER) >= 1);
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(finance.balance(AccountCode.ASSET_RECEIVABLE.getCode())));
        finance.assertNoIncomePayments();
    }

    @Test
    void golden_payrollExpenseThenPaymentSettlement_noDuplicateExpense() {
        finance.finance().postTransaction(TransactionType.EXPENSE, new BigDecimal("20000.00"),
                BusinessUnit.GENERAL, AccountCode.EXP_SALARIES.getCode(),
                AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                "payroll accrual", "HR", "PAYROLL_GROSS", 300L);
        finance.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("20000.00"),
                BusinessUnit.GENERAL, AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                AccountCode.ASSET_BANK.getCode(),
                "payroll disbursement", "HR", "PAYROLL_PAYMENT", 400L);

        assertEquals(1L, finance.countByType(TransactionType.EXPENSE));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(
                finance.balance(AccountCode.LIABILITY_SALARY_PAYABLE.getCode())));
        finance.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("20000.00"),
                BusinessUnit.GENERAL, AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                AccountCode.ASSET_BANK.getCode(),
                "payroll disbursement retry", "HR", "PAYROLL_PAYMENT", 400L);
        assertEquals(1L, finance.ledger().stream()
                .filter(t -> "PAYROLL_PAYMENT".equals(t.getReferenceType())).count());
        finance.assertNoIncomePayments();
    }
}
