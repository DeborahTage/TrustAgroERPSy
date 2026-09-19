package com.trustagro.farm.delivery.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.farm.delivery.dto.*;
import com.trustagro.farm.delivery.entity.ChickenDeliveryRelease;
import com.trustagro.farm.delivery.entity.ChickenReleaseStatus;
import com.trustagro.farm.delivery.repository.ChickenDeliveryReleaseRepository;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSalePayment;
import com.trustagro.farm.sale.entity.FlockPopulationEvent;
import com.trustagro.farm.sale.entity.FlockPopulationEventType;
import com.trustagro.farm.sale.repository.ChickenSalePaymentRepository;
import com.trustagro.farm.sale.repository.ChickenSaleRepository;
import com.trustagro.farm.sale.repository.FlockPopulationEventRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.farm.service.FarmNumberService;
import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.RoleCompatibility;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChickenDeliveryReleaseService {

    public static final String AUDIT_MODULE = "CHICKEN_RELEASE";

    private static final List<ChickenReleaseStatus> FINANCE_QUEUE = List.of(
            ChickenReleaseStatus.SUBMITTED,
            ChickenReleaseStatus.UNDER_REVIEW,
            ChickenReleaseStatus.VERIFIED
    );
    private static final List<ChickenReleaseStatus> FARM_QUEUE = List.of(
            ChickenReleaseStatus.FORWARDED_TO_FARM,
            ChickenReleaseStatus.FARM_PENDING,
            ChickenReleaseStatus.FARM_APPROVED
    );
    private static final List<ChickenReleaseStatus> MARKETING_QUEUE = List.of(
            ChickenReleaseStatus.DISPATCHED,
            ChickenReleaseStatus.MARKETING_RECEIVED,
            ChickenReleaseStatus.OUT_FOR_DELIVERY,
            ChickenReleaseStatus.DELIVERED,
            ChickenReleaseStatus.DELIVERY_FAILED
    );

    private final ChickenDeliveryReleaseRepository releaseRepository;
    private final ChickenSaleRepository saleRepository;
    private final ChickenSalePaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final FlockRepository flockRepository;
    private final CrmClientRepository clientRepository;
    private final FlockPopulationEventRepository populationEventRepository;
    private final FarmNumberService farmNumberService;
    private final FarmAccessService farmAccessService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ChickenDeliveryReleaseResponse> list(ChickenReleaseStatus status, String queue) {
        List<ChickenDeliveryRelease> rows;
        if (queue != null && !queue.isBlank()) {
            List<ChickenReleaseStatus> statuses = switch (queue.trim().toUpperCase()) {
                case "FINANCE" -> FINANCE_QUEUE;
                case "FARM" -> FARM_QUEUE;
                case "MARKETING" -> MARKETING_QUEUE;
                default -> throw new BusinessException("Unknown queue: " + queue);
            };
            rows = releaseRepository.findByStatusInOrderByCreatedAtDesc(statuses);
        } else if (status != null) {
            rows = releaseRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            rows = releaseRepository.findAll();
        }
        return rows.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChickenDeliveryReleaseResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ChickenDeliveryReleaseResponse create(ChickenDeliveryReleaseRequest req) {
        User user = farmAccessService.currentUser();
        ChickenSale sale = saleRepository.findById(req.getChickenSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Chicken sale not found"));

        validatePaymentBasis(sale, req);

        int authorized = req.getAuthorizedChickenQuantity();
        int already = releaseRepository.sumAuthorizedForSale(sale.getId());
        int orderQty = sale.totalChickens() != null ? sale.totalChickens() : 0;
        if (authorized > orderQty - already) {
            throw new BusinessException("Authorized quantity exceeds remaining order quantity ("
                    + (orderQty - already) + ")");
        }

        ChickenDeliveryRelease release = new ChickenDeliveryRelease();
        release.setReleaseNumber(farmNumberService.nextPaymentVerification());
        release.setChickenSale(sale);
        release.setFarm(sale.getFarm());
        release.setFlock(sale.getFlock());
        release.setCustomer(sale.getCustomer());
        release.setInvoice(sale.getInvoice());
        release.setAuthorizedChickenQuantity(authorized);
        release.setBonusQuantity(req.getBonusQuantity() != null ? req.getBonusQuantity() : 0);
        release.setChickenType(req.getChickenType());
        release.setPaymentReceiptUrl(req.getPaymentReceiptUrl());
        release.setPaymentAmount(req.getPaymentAmount() != null ? req.getPaymentAmount() : sale.getPaidAmount());
        release.setNotes(req.getNotes());
        release.setCreatedBy(user);
        release.setStatus(ChickenReleaseStatus.SUBMITTED);
        // Legacy ChickenSale.confirm() already decrements flock. When a release is created
        // for a post-confirm sale, dispatch must not decrement again — physical birds left at confirm.
        // New Step-17 path still decrements at dispatch when sale is still DRAFT (pre-confirm).
        boolean alreadyTakenAtConfirm = sale.getStatus() != null
                && sale.getStatus() != com.trustagro.farm.sale.entity.ChickenSaleStatus.DRAFT
                && sale.getStatus() != com.trustagro.farm.sale.entity.ChickenSaleStatus.CANCELLED;
        release.setStockDecremented(alreadyTakenAtConfirm);

        if (req.getChickenSalePaymentId() != null) {
            ChickenSalePayment payment = paymentRepository.findById(req.getChickenSalePaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chicken sale payment not found"));
            if (!payment.getSale().getId().equals(sale.getId())) {
                throw new BusinessException("Payment does not belong to the selected sale");
            }
            release.setChickenSalePayment(payment);
            if (req.getPaymentAmount() == null) {
                release.setPaymentAmount(payment.getAmount());
            }
        }
        if (req.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository.findById(req.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
            release.setInvoice(invoice);
        }

        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("CREATE", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(), null, saved.getReleaseNumber());
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse verify(Long id) {
        ChickenDeliveryRelease release = find(id);
        if (release.getStatus() != ChickenReleaseStatus.SUBMITTED
                && release.getStatus() != ChickenReleaseStatus.UNDER_REVIEW) {
            throw new BusinessException("Only SUBMITTED/UNDER_REVIEW releases can be verified");
        }
        String old = release.getStatus().name();
        User user = farmAccessService.currentUser();
        release.setStatus(ChickenReleaseStatus.VERIFIED);
        release.setVerifiedBy(user);
        release.setVerifiedAt(LocalDateTime.now());
        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("VERIFY", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse reject(Long id, RejectionRequest req) {
        ChickenDeliveryRelease release = find(id);
        if (release.getStatus() == ChickenReleaseStatus.DISPATCHED
                || release.getStatus() == ChickenReleaseStatus.DELIVERED
                || release.getStatus() == ChickenReleaseStatus.CANCELLED) {
            throw new BusinessException("Cannot reject release in status " + release.getStatus());
        }
        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("Rejection reason is required");
        }
        String old = release.getStatus().name();
        release.setStatus(ChickenReleaseStatus.REJECTED);
        release.setFarmRejectionReason(req.getReason());
        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("REJECT", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse forwardToFarm(Long id) {
        ChickenDeliveryRelease release = find(id);
        if (release.getStatus() != ChickenReleaseStatus.VERIFIED) {
            throw new BusinessException("Only VERIFIED releases can be forwarded to farm");
        }
        String old = release.getStatus().name();
        release.setStatus(ChickenReleaseStatus.FORWARDED_TO_FARM);
        ChickenDeliveryRelease saved = releaseRepository.save(release);
        // Move to farm pending for queue visibility
        saved.setStatus(ChickenReleaseStatus.FARM_PENDING);
        saved = releaseRepository.save(saved);
        auditService.log("FORWARD_TO_FARM", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(),
                old, saved.getStatus().name());

        notifyFarmManagers(saved, "Chicken release pending farm approval",
                "Release " + saved.getReleaseNumber() + " awaits farm approval");
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse farmApprove(Long id) {
        ChickenDeliveryRelease release = find(id);
        User user = farmAccessService.currentUser();
        Farm farm = release.getFarm() != null ? release.getFarm() : release.getChickenSale().getFarm();
        farmAccessService.assertCanAccessFarm(user, farm.getId());
        assertFarmManagerOrGlobal(user);

        if (release.getStatus() != ChickenReleaseStatus.FORWARDED_TO_FARM
                && release.getStatus() != ChickenReleaseStatus.FARM_PENDING) {
            throw new BusinessException("Release is not awaiting farm approval");
        }
        String old = release.getStatus().name();
        release.setStatus(ChickenReleaseStatus.FARM_APPROVED);
        release.setFarmApprovedBy(user);
        release.setFarmApprovedAt(LocalDateTime.now());
        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("FARM_APPROVE", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(),
                old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse farmReject(Long id, RejectionRequest req) {
        ChickenDeliveryRelease release = find(id);
        User user = farmAccessService.currentUser();
        Farm farm = release.getFarm() != null ? release.getFarm() : release.getChickenSale().getFarm();
        farmAccessService.assertCanAccessFarm(user, farm.getId());
        assertFarmManagerOrGlobal(user);

        if (req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessException("Farm rejection reason is required");
        }
        if (release.getStatus() != ChickenReleaseStatus.FORWARDED_TO_FARM
                && release.getStatus() != ChickenReleaseStatus.FARM_PENDING) {
            throw new BusinessException("Release is not awaiting farm approval");
        }
        String old = release.getStatus().name();
        release.setStatus(ChickenReleaseStatus.FARM_REJECTED);
        release.setFarmRejectionReason(req.getReason());
        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("FARM_REJECT", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(),
                old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse dispatch(Long id, ChickenDispatchRequest req) {
        ChickenDeliveryRelease release = find(id);
        User user = farmAccessService.currentUser();
        Farm farm = release.getFarm() != null ? release.getFarm() : release.getChickenSale().getFarm();
        farmAccessService.assertCanAccessFarm(user, farm.getId());
        assertFarmManagerOrGlobal(user);

        if (release.getStatus() != ChickenReleaseStatus.FARM_APPROVED) {
            throw new BusinessException("Only FARM_APPROVED releases can be dispatched");
        }

        Long flockId = req.getFlockId() != null ? req.getFlockId()
                : (release.getFlock() != null ? release.getFlock().getId() : release.getChickenSale().getFlock().getId());
        Flock flock = flockRepository.findByIdForUpdate(flockId)
                .orElseThrow(() -> new ResourceNotFoundException("Flock not found"));

        String chickenType = req.getChickenType() != null ? req.getChickenType() : release.getChickenType();
        if (chickenType != null && flock.getBirdType() != null
                && !chickenType.equalsIgnoreCase(flock.getBirdType())) {
            throw new BusinessException("Chicken type does not match flock bird type (" + flock.getBirdType() + ")");
        }

        int bonus = req.getBonusQuantity() != null ? req.getBonusQuantity() : (release.getBonusQuantity() != null ? release.getBonusQuantity() : 0);
        if (bonus < 0) {
            throw new BusinessException("Bonus quantity cannot be negative");
        }
        int authorized = release.getAuthorizedChickenQuantity();
        int totalPhysical = authorized + bonus;
        int available = flock.getCurrentBirdCount() != null ? flock.getCurrentBirdCount() : 0;
        if (totalPhysical > available) {
            throw new BusinessException("Insufficient birds: need " + totalPhysical + ", flock has " + available);
        }

        if (req.getCustomerId() != null) {
            CrmClient customer = clientRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            release.setCustomer(customer);
        }

        // Decrement flock population exactly once — no finance revenue for bonus or dispatch
        if (!release.isStockDecremented()) {
            int previous = available;
            int remaining = available - totalPhysical;
            flock.setCurrentBirdCount(remaining);
            if (remaining > 0) {
                flock.setStatus(FlockStatus.PARTIALLY_SOLD);
            } else {
                flock.setStatus(FlockStatus.SOLD);
            }
            flockRepository.save(flock);

            FlockPopulationEvent event = new FlockPopulationEvent();
            event.setFlock(flock);
            event.setEventType(FlockPopulationEventType.DISPATCH);
            event.setPreviousPopulation(previous);
            event.setQuantityChange(totalPhysical);
            event.setNewPopulation(remaining);
            event.setChickenSaleId(release.getChickenSale().getId());
            event.setEventDate(req.getDispatchDate() != null ? req.getDispatchDate() : LocalDate.now());
            event.setRecordedBy(user);
            event.setNotes("Dispatch " + release.getReleaseNumber()
                    + " authorized=" + authorized + " bonus=" + bonus + " (no revenue)");
            populationEventRepository.save(event);
            release.setStockDecremented(true);
        }

        String old = release.getStatus().name();
        release.setFlock(flock);
        release.setChickenType(chickenType);
        release.setBonusQuantity(bonus);
        release.setDispatchDate(req.getDispatchDate() != null ? req.getDispatchDate() : LocalDate.now());
        release.setDispatchedBy(user);
        release.setDispatchedAt(LocalDateTime.now());
        release.setStatus(ChickenReleaseStatus.DISPATCHED);
        if (req.getNotes() != null) {
            release.setNotes(req.getNotes());
        }

        ChickenDeliveryRelease saved = releaseRepository.save(release);
        auditService.log("DISPATCH", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(),
                old, saved.getStatus().name());

        notificationService.createRoleNotification(
                "Chicken dispatch ready",
                "Release " + saved.getReleaseNumber() + " dispatched — marketing logistics",
                Role.MARKETING_MANAGER);

        return toResponse(saved);
    }

    @Transactional
    public ChickenDeliveryReleaseResponse marketingReceive(Long id) {
        return updateLogistics(id, logistics("MARKETING_RECEIVED", null, null));
    }

    @Transactional
    public ChickenDeliveryReleaseResponse markDelivered(Long id) {
        return updateLogistics(id, logistics("DELIVERED", null, "DELIVERED"));
    }

    @Transactional
    public ChickenDeliveryReleaseResponse updateLogistics(Long id, MarketingLogisticsRequest req) {
        ChickenDeliveryRelease release = find(id);
        Set<ChickenReleaseStatus> allowedFrom = EnumSet.of(
                ChickenReleaseStatus.DISPATCHED,
                ChickenReleaseStatus.MARKETING_RECEIVED,
                ChickenReleaseStatus.OUT_FOR_DELIVERY,
                ChickenReleaseStatus.DELIVERY_FAILED
        );
        if (!allowedFrom.contains(release.getStatus()) && release.getStatus() != ChickenReleaseStatus.DELIVERED) {
            throw new BusinessException("Marketing cannot update logistics from status " + release.getStatus());
        }

        String old = release.getStatus().name();
        if (req.getMarketingNotes() != null) {
            release.setMarketingNotes(req.getMarketingNotes());
        }
        if (req.getDeliveryOutcome() != null) {
            release.setDeliveryOutcome(req.getDeliveryOutcome());
        }
        if (req.getLogisticsStatus() != null && !req.getLogisticsStatus().isBlank()) {
            ChickenReleaseStatus next = ChickenReleaseStatus.valueOf(req.getLogisticsStatus().trim().toUpperCase());
            Set<ChickenReleaseStatus> logistics = EnumSet.of(
                    ChickenReleaseStatus.MARKETING_RECEIVED,
                    ChickenReleaseStatus.OUT_FOR_DELIVERY,
                    ChickenReleaseStatus.DELIVERED,
                    ChickenReleaseStatus.DELIVERY_FAILED
            );
            if (!logistics.contains(next)) {
                throw new BusinessException("Invalid logistics status: " + next);
            }
            release.setStatus(next);
        }

        ChickenDeliveryRelease saved = releaseRepository.save(release);
        if (!old.equals(saved.getStatus().name())) {
            auditService.log("LOGISTICS", AUDIT_MODULE, "ChickenDeliveryRelease", saved.getId(),
                    old, saved.getStatus().name());
        }
        return toResponse(saved);
    }

    private MarketingLogisticsRequest logistics(String status, String notes, String outcome) {
        MarketingLogisticsRequest req = new MarketingLogisticsRequest();
        req.setLogisticsStatus(status);
        req.setMarketingNotes(notes);
        req.setDeliveryOutcome(outcome);
        return req;
    }

    private void validatePaymentBasis(ChickenSale sale, ChickenDeliveryReleaseRequest req) {
        boolean hasInvoiceAndPaid = sale.getInvoice() != null
                && sale.getPaidAmount() != null
                && sale.getPaidAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasPaymentLink = req.getChickenSalePaymentId() != null;
        if (!hasInvoiceAndPaid && !hasPaymentLink) {
            throw new BusinessException("Release requires an invoiced sale with paidAmount > 0 or a linked payment");
        }
    }

    private void assertFarmManagerOrGlobal(User user) {
        Role role = RoleCompatibility.normalize(user.getRole());
        if (role == Role.ADMIN || role == Role.GENERAL_MANAGER || role == Role.OPERATIONS_MANAGER
                || role == Role.FARM_MANAGER || farmAccessService.hasGlobalFarmAccess(user)) {
            return;
        }
        throw new BusinessException("Only Farm Manager (or global farm access) can perform this action");
    }

    private void notifyFarmManagers(ChickenDeliveryRelease release, String title, String message) {
        Farm farm = release.getFarm();
        if (farm != null && farm.getAssignedFarmManager() != null) {
            notificationService.createUserNotification(title, message, farm.getAssignedFarmManager());
        }
        notificationService.createRoleNotification(title, message, Role.FARM_MANAGER);
    }

    private ChickenDeliveryRelease find(Long id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chicken release not found: " + id));
    }

    ChickenDeliveryReleaseResponse toResponse(ChickenDeliveryRelease r) {
        ChickenDeliveryReleaseResponse res = new ChickenDeliveryReleaseResponse();
        res.setId(r.getId());
        res.setReleaseNumber(r.getReleaseNumber());
        if (r.getChickenSale() != null) {
            res.setChickenSaleId(r.getChickenSale().getId());
            res.setSaleNumber(r.getChickenSale().getSaleNumber());
        }
        if (r.getChickenSalePayment() != null) {
            res.setChickenSalePaymentId(r.getChickenSalePayment().getId());
        }
        if (r.getInvoice() != null) {
            res.setInvoiceId(r.getInvoice().getId());
        }
        if (r.getFarm() != null) {
            res.setFarmId(r.getFarm().getId());
            res.setFarmName(r.getFarm().getFarmName());
        }
        if (r.getFlock() != null) {
            res.setFlockId(r.getFlock().getId());
            res.setFlockBatchCode(r.getFlock().getBatchCode());
        }
        if (r.getCustomer() != null) {
            res.setCustomerId(r.getCustomer().getId());
            res.setCustomerName(r.getCustomer().getClientName());
        }
        res.setPaymentAmount(r.getPaymentAmount());
        res.setAuthorizedChickenQuantity(r.getAuthorizedChickenQuantity());
        res.setBonusQuantity(r.getBonusQuantity());
        res.setChickenType(r.getChickenType());
        res.setPaymentReceiptUrl(r.getPaymentReceiptUrl());
        res.setStatus(r.getStatus());
        if (r.getVerifiedBy() != null) {
            res.setVerifiedByName(r.getVerifiedBy().getFullName());
        }
        res.setVerifiedAt(r.getVerifiedAt());
        if (r.getFarmApprovedBy() != null) {
            res.setFarmApprovedByName(r.getFarmApprovedBy().getFullName());
        }
        res.setFarmApprovedAt(r.getFarmApprovedAt());
        res.setFarmRejectionReason(r.getFarmRejectionReason());
        if (r.getDispatchedBy() != null) {
            res.setDispatchedByName(r.getDispatchedBy().getFullName());
        }
        res.setDispatchedAt(r.getDispatchedAt());
        res.setDispatchDate(r.getDispatchDate());
        res.setMarketingNotes(r.getMarketingNotes());
        res.setDeliveryOutcome(r.getDeliveryOutcome());
        res.setStockDecremented(r.isStockDecremented());
        res.setNotes(r.getNotes());
        if (r.getCreatedBy() != null) {
            res.setCreatedByName(r.getCreatedBy().getFullName());
        }
        res.setVersion(r.getVersion());
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        return res;
    }
}
