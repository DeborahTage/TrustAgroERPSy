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
        position(code, name, null);
    }

    private void position(String code, String name, String description) {
        HrPosition p = positionRepository.findByCode(code).orElseGet(() -> {
            HrPosition newP = new HrPosition();
            newP.setCode(code);
            newP.setActive(true);
            return newP;
        });
        p.setName(name);
        if (description != null) {
            p.setDescription(description);
        }
        positionRepository.save(p);
    }

    private void seedPositions() {
        position("GENERAL_MANAGER", "General Manager (Trust Agro Consulting & Farming)",
                "Key Responsibilities:\n" +
                "- Set direction: Define goals for farming and consulting\n" +
                "- Oversee operations: Ensure all departments work well\n" +
                "- Control finance: Monitor cost, revenue, and profit\n" +
                "- Ensure quality: Maintain farm performance and service standards\n" +
                "- Lead team: Supervise managers and staff\n" +
                "- Develop business: Manage clients and grow the company\n" +
                "- Make decisions: Final authority on major issues");
        position("OPERATIONS_MANAGER", "Operations Manager",
                "Key Responsibilities:\n" +
                "- Run daily operations: Ensure farm and consulting activities run smoothly\n" +
                "- Coordinate teams: Link farm, veterinary, store, and sales departments\n" +
                "- Implement plans: Turn GM strategy into daily actions\n" +
                "- Monitor performance: Track production, service delivery, and efficiency\n" +
                "- Solve problems: Handle operational issues quickly (farm, logistics, staff)\n" +
                "- Standardize processes: Ensure all sites follow the same procedures");
        position("SECRETARY", "Secretary",
                "Key Responsibilities:\n" +
                "- Manage communication: Handle calls, emails, and messages with staff and clients\n" +
                "- Schedule activities: Arrange meetings, farm visits, and trainings\n" +
                "- Keep records: Maintain documents, reports, and contracts\n" +
                "- Support management: Assist GM and managers with coordination\n" +
                "- Follow-up: Track tasks, deadlines, and ensure actions are completed");
        position("MARKETING_MANAGER", "Marketing Team (Trust Agro Consulting)",
                "Key Responsibilities:\n" +
                "- Generate clients: Find and attract new farmers and business partners\n" +
                "- Promote services: Advertise consulting, veterinary support, and farm solutions\n" +
                "- Build brand: Create trust and visibility in the market\n" +
                "- Manage channels: Handle social media, field promotion, and campaigns\n" +
                "- Support sales: Convert leads into paying clients\n" +
                "- Collect feedback: Understand customer needs and improve services");
        position("MARKETING_OFFICER", "Marketing Team (Trust Agro Consulting)",
                "Key Responsibilities:\n" +
                "- Generate clients: Find and attract new farmers and business partners\n" +
                "- Promote services: Advertise consulting, veterinary support, and farm solutions\n" +
                "- Build brand: Create trust and visibility in the market\n" +
                "- Manage channels: Handle social media, field promotion, and campaigns\n" +
                "- Support sales: Convert leads into paying clients\n" +
                "- Collect feedback: Understand customer needs and improve services");
        position("PHARMACIST", "Pharmacy Sales (Trust Agro)",
                "Key Responsibilities:\n" +
                "- Sell products: Veterinary drugs, vaccines, and farm inputs\n" +
                "- Advise customers: Guide proper drug use (dose, purpose, safety)\n" +
                "- Manage customers: Build relationships and retain clients\n" +
                "- Record sales: Track daily sales and report revenue\n" +
                "- Coordinate with vet team: Ensure correct prescriptions and usage\n" +
                "- Monitor stock: Work with store keeper to avoid shortages or expiry");
        position("PHARMACY_SALES", "Pharmacy Sales (Trust Agro)",
                "Key Responsibilities:\n" +
                "- Sell products: Veterinary drugs, vaccines, and farm inputs\n" +
                "- Advise customers: Guide proper drug use (dose, purpose, safety)\n" +
                "- Manage customers: Build relationships and retain clients\n" +
                "- Record sales: Track daily sales and report revenue\n" +
                "- Coordinate with vet team: Ensure correct prescriptions and usage\n" +
                "- Monitor stock: Work with store keeper to avoid shortages or expiry");
        position("FARM_MANAGER", "Farm Manager");
        position("FARM_WORKER", "Farm Worker");
        position("VETERINARIAN", "Veterinarian");
        position("INVENTORY_MANAGER", "Inventory Manager");
        position("ACCOUNTANT", "Accountant");
        position("HR_OFFICER", "HR Officer");
        position("CRM_OFFICER", "CRM Officer");
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
