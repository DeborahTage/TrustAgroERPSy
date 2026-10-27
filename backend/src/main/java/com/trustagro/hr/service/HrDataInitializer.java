package com.trustagro.hr.service;

import com.trustagro.hr.entity.*;
import com.trustagro.hr.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Idempotent HR reference-data and number-sequence initializer.
 * <p>
 * HR number sequences are initialized on every startup to ensure system counters
 * are always ready and business operations are never blocked.
 * <p>
 * Departments, positions, leave entitlements, and deduction rates are optional
 * reference-data seeds controlled by {@code app.seed.hr-reference-data} (default {@code false}).
 * In production, fictional or unverified HR settings must not be silently initialized.
 * <p>
 * Any deduction rates seeded for local development are explicitly marked as
 * unapproved placeholders ({@code approved = false}) with disclaimer notes.
 * The production payroll safety rule strictly prevents payroll finalization or
 * payment when unapproved or placeholder deduction rates exist.
 */
@Component
public class HrDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HrDataInitializer.class);
    private static final String RATE_DISCLAIMER =
            "PLACEHOLDER RATE — not a statutory figure. Configure per the regulations that apply before running payroll.";

    private final HrDepartmentRepository departmentRepository;
    private final HrPositionRepository positionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final PayrollDeductionTypeRepository deductionTypeRepository;
    private final HrNumberSequenceRepository sequenceRepository;
    private final boolean seedHrReferenceData;

    public HrDataInitializer(
            HrDepartmentRepository departmentRepository,
            HrPositionRepository positionRepository,
            LeaveTypeRepository leaveTypeRepository,
            PayrollDeductionTypeRepository deductionTypeRepository,
            HrNumberSequenceRepository sequenceRepository,
            @Value("${app.seed.hr-reference-data:false}") boolean seedHrReferenceData) {
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.deductionTypeRepository = deductionTypeRepository;
        this.sequenceRepository = sequenceRepository;
        this.seedHrReferenceData = seedHrReferenceData;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedSequences();
        if (seedHrReferenceData) {
            log.info("Seeding optional HR reference data (departments, positions, leave types, placeholder deductions)");
            seedDepartments();
            seedPositions();
            seedLeaveTypes();
            seedDeductionTypes();
        } else {
            log.info("HR reference-data seeding disabled (app.seed.hr-reference-data=false)");
        }
    }

    private void seedSequences() {
        for (String name : List.of("EMPLOYEE", "LEAVE", "PAYROLL_PERIOD", "PAYROLL_PAYMENT", "PAYSLIP")) {
            if (sequenceRepository.findById(name).isEmpty()) {
                HrNumberSequence seq = new HrNumberSequence();
                seq.setName(name);
                seq.setNextValue(1L);
                sequenceRepository.save(seq);
            }
        }
    }

    private void seedDepartments() {
        department("MANAGEMENT", "Management");
        department("FARM_OPERATIONS", "Farm Operations");
        department("VETERINARY", "Veterinary");
        department("PHARMACY", "Pharmacy");
        department("INVENTORY", "Inventory");
        department("FINANCE", "Finance");
        department("HR", "Human Resources");
        department("CRM", "Customer Relations");
        department("MARKETING", "Marketing");
        department("TRAINING", "Training");
        department("CONSULTANCY", "Consultancy");
        department("ADMINISTRATION", "Administration");
    }

    private void position(String code, String name) {
        if (positionRepository.existsByCode(code)) {
            return;
        }
        HrPosition p = new HrPosition();
        p.setCode(code);
        p.setName(name);
        p.setActive(true);
        positionRepository.save(p);
    }

    private void seedPositions() {
        position("GENERAL_MANAGER", "General Manager");
        position("OPERATIONS_MANAGER", "Operations Manager");
        position("FARM_MANAGER", "Farm Manager");
        position("FARM_WORKER", "Farm Worker");
        position("VETERINARIAN", "Veterinarian");
        position("PHARMACIST", "Pharmacist");
        position("INVENTORY_MANAGER", "Inventory Manager");
        position("ACCOUNTANT", "Accountant");
        position("HR_OFFICER", "HR Officer");
        position("CRM_OFFICER", "CRM Officer");
        position("MARKETING_OFFICER", "Marketing Officer");
        position("TRAINING_COORDINATOR", "Training Coordinator");
        position("CONSULTANT", "Consultant");
        position("EXTENSION_WORKER", "Extension Worker");
        position("ADMIN_ASSISTANT", "Administrative Assistant");
    }

    private void seedLeaveTypes() {
        leaveType("ANNUAL", "Annual Leave", true, 18, true,
                "Default annual entitlement is a configurable placeholder, not a statutory figure.");
        leaveType("SICK", "Sick Leave", true, 10, true,
                "Default sick entitlement is a configurable placeholder, not a statutory figure.");
        leaveType("MATERNITY", "Maternity Leave", true, 90, true,
                "Default maternity entitlement is a configurable placeholder, not a statutory figure.");
        leaveType("EMERGENCY", "Emergency Leave", true, 5, true,
                "Discretionary short-notice leave. Entitlement is configurable.");
        leaveType("UNPAID", "Unpaid Leave", false, 0, false,
                "Not balance controlled and not paid.");
        leaveType("OTHER", "Other Leave", false, 0, true,
                "Catch-all for leave that is tracked but not balance controlled.");
    }

    private void seedDeductionTypes() {
        deductionType("INCOME_TAX", "Income Tax", DeductionCategory.INCOME_TAX, new BigDecimal("15.0000"));
        deductionType("PENSION", "Pension Contribution", DeductionCategory.PENSION, new BigDecimal("7.0000"));
    }

    private void department(String code, String name) {
        if (departmentRepository.existsByCode(code)) {
            return;
        }
        HrDepartment d = new HrDepartment();
        d.setCode(code);
        d.setName(name);
        d.setActive(true);
        departmentRepository.save(d);
    }

    private void leaveType(String code, String name, boolean balanceControlled,
                           int entitlementDays, boolean paid, String note) {
        if (leaveTypeRepository.existsByCode(code)) {
            return;
        }
        LeaveType t = new LeaveType();
        t.setCode(code);
        t.setName(name);
        t.setBalanceControlled(balanceControlled);
        t.setDefaultAnnualEntitlement(new BigDecimal(entitlementDays).setScale(2));
        t.setPaid(paid);
        t.setActive(true);
        t.setDescription(note);
        leaveTypeRepository.save(t);
    }

    private void deductionType(String code, String name, DeductionCategory category, BigDecimal ratePercent) {
        if (deductionTypeRepository.existsByCode(code)) {
            return;
        }
        PayrollDeductionType d = new PayrollDeductionType();
        d.setCode(code);
        d.setName(name);
        d.setCategory(category);
        d.setRatePercent(ratePercent);
        d.setActive(true);
        d.setApproved(false);
        d.setNotes(RATE_DISCLAIMER);
        deductionTypeRepository.save(d);
    }
}
