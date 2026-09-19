package com.trustagro.farm.delivery.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.farm.delivery.dto.InternalStockDeliveryRequest;
import com.trustagro.farm.delivery.dto.InternalStockDeliveryResponse;
import com.trustagro.farm.delivery.entity.InternalDeliveryStatus;
import com.trustagro.farm.delivery.entity.InternalStockDelivery;
import com.trustagro.farm.delivery.repository.InternalStockDeliveryRepository;
import com.trustagro.farm.service.FarmAccessService;
import com.trustagro.farm.service.FarmNumberService;
import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalStockDeliveryService {

    public static final String AUDIT_MODULE = "INTERNAL_DELIVERY";

    private static final Map<InternalDeliveryStatus, Set<InternalDeliveryStatus>> TRANSITIONS = Map.of(
            InternalDeliveryStatus.DRAFT, EnumSet.of(InternalDeliveryStatus.SUBMITTED, InternalDeliveryStatus.CANCELLED),
            InternalDeliveryStatus.SUBMITTED, EnumSet.of(InternalDeliveryStatus.APPROVED, InternalDeliveryStatus.CANCELLED),
            InternalDeliveryStatus.APPROVED, EnumSet.of(InternalDeliveryStatus.ISSUED, InternalDeliveryStatus.CANCELLED),
            InternalDeliveryStatus.ISSUED, EnumSet.of(InternalDeliveryStatus.RECEIVED, InternalDeliveryStatus.CANCELLED),
            InternalDeliveryStatus.RECEIVED, EnumSet.noneOf(InternalDeliveryStatus.class),
            InternalDeliveryStatus.CANCELLED, EnumSet.noneOf(InternalDeliveryStatus.class)
    );

    private final InternalStockDeliveryRepository deliveryRepository;
    private final InventoryItemRepository itemRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final FarmNumberService farmNumberService;
    private final FarmAccessService farmAccessService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<InternalStockDeliveryResponse> list(InternalDeliveryStatus status) {
        List<InternalStockDelivery> rows = status != null
                ? deliveryRepository.findByStatusOrderByCreatedAtDesc(status)
                : deliveryRepository.findAll();
        return rows.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalStockDeliveryResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public InternalStockDeliveryResponse create(InternalStockDeliveryRequest req) {
        User user = farmAccessService.currentUser();
        InternalStockDelivery d = new InternalStockDelivery();
        d.setReceiptNumber(farmNumberService.nextInternalDeliveryReceipt());
        d.setStatus(InternalDeliveryStatus.DRAFT);
        d.setCreatedBy(user);
        applyFields(d, req);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log("CREATE", AUDIT_MODULE, "InternalStockDelivery", saved.getId(), null, saved.getReceiptNumber());
        return toResponse(saved);
    }

    @Transactional
    public InternalStockDeliveryResponse update(Long id, InternalStockDeliveryRequest req) {
        InternalStockDelivery d = find(id);
        if (d.getStatus() != InternalDeliveryStatus.DRAFT) {
            throw new BusinessException("Only DRAFT deliveries can be updated");
        }
        applyFields(d, req);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log("UPDATE", AUDIT_MODULE, "InternalStockDelivery", saved.getId(),
                InternalDeliveryStatus.DRAFT.name(), saved.getReceiptNumber());
        return toResponse(saved);
    }

    @Transactional
    public InternalStockDeliveryResponse submit(Long id) {
        return transition(id, InternalDeliveryStatus.SUBMITTED, "SUBMIT", null);
    }

    @Transactional
    public InternalStockDeliveryResponse approve(Long id) {
        InternalStockDelivery d = find(id);
        User user = farmAccessService.currentUser();
        assertTransition(d.getStatus(), InternalDeliveryStatus.APPROVED);
        if (d.getReceiver() != null && d.getReceiver().getId().equals(user.getId())) {
            throw new BusinessException("Receiver cannot approve their own delivery (SoD)");
        }
        String old = d.getStatus().name();
        d.setStatus(InternalDeliveryStatus.APPROVED);
        d.setApprovedBy(user);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log("APPROVE", AUDIT_MODULE, "InternalStockDelivery", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public InternalStockDeliveryResponse issue(Long id) {
        InternalStockDelivery d = find(id);
        User user = farmAccessService.currentUser();
        assertTransition(d.getStatus(), InternalDeliveryStatus.ISSUED);

        if (d.getStockMovementId() == null) {
            if (d.getSourceLocation() == null || d.getDestinationLocation() == null) {
                throw new BusinessException("Source and destination locations are required to issue");
            }
            inventoryService.transferStock(
                    d.getInventoryItem().getId(),
                    d.getSourceLocation(),
                    d.getDestinationLocation(),
                    d.getQuantity().doubleValue(),
                    user);
            d.setStockMovementId(d.getId() != null ? d.getId() : System.currentTimeMillis());
        }

        String old = d.getStatus().name();
        d.setStatus(InternalDeliveryStatus.ISSUED);
        d.setIssuedBy(user);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log("ISSUE", AUDIT_MODULE, "InternalStockDelivery", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public InternalStockDeliveryResponse receive(Long id) {
        InternalStockDelivery d = find(id);
        User user = farmAccessService.currentUser();
        assertTransition(d.getStatus(), InternalDeliveryStatus.RECEIVED);
        String old = d.getStatus().name();
        d.setStatus(InternalDeliveryStatus.RECEIVED);
        d.setReceivedBy(user);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log("RECEIVE", AUDIT_MODULE, "InternalStockDelivery", saved.getId(), old, saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public InternalStockDeliveryResponse cancel(Long id) {
        return transition(id, InternalDeliveryStatus.CANCELLED, "CANCEL", null);
    }

    private InternalStockDeliveryResponse transition(Long id, InternalDeliveryStatus target, String action, User actor) {
        InternalStockDelivery d = find(id);
        User user = actor != null ? actor : farmAccessService.currentUser();
        assertTransition(d.getStatus(), target);
        String old = d.getStatus().name();
        d.setStatus(target);
        InternalStockDelivery saved = deliveryRepository.save(d);
        auditService.log(action, AUDIT_MODULE, "InternalStockDelivery", saved.getId(), old, target.name());
        return toResponse(saved);
    }

    private void applyFields(InternalStockDelivery d, InternalStockDeliveryRequest req) {
        InventoryItem item = itemRepository.findById(req.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        d.setInventoryItem(item);
        d.setDeliveryDate(req.getDeliveryDate() != null ? req.getDeliveryDate() : LocalDate.now());
        d.setUnit(req.getUnit() != null ? req.getUnit() : (item.getUnit() != null ? item.getUnit().name() : null));
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Quantity must be positive");
        }
        d.setQuantity(req.getQuantity());
        d.setSourceLocation(req.getSourceLocation());
        d.setDestinationLocation(req.getDestinationLocation());
        d.setPurpose(req.getPurpose());
        d.setNotes(req.getNotes());
        if (req.getDelivererId() != null) {
            d.setDeliverer(userRepository.findById(req.getDelivererId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deliverer not found")));
        }
        if (req.getReceiverId() != null) {
            d.setReceiver(userRepository.findById(req.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver not found")));
        }
    }

    private void assertTransition(InternalDeliveryStatus from, InternalDeliveryStatus to) {
        Set<InternalDeliveryStatus> allowed = TRANSITIONS.getOrDefault(from, EnumSet.noneOf(InternalDeliveryStatus.class));
        if (!allowed.contains(to)) {
            throw new BusinessException("Cannot transition internal delivery from " + from + " to " + to);
        }
    }

    private InternalStockDelivery find(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internal delivery not found: " + id));
    }

    InternalStockDeliveryResponse toResponse(InternalStockDelivery d) {
        InternalStockDeliveryResponse r = new InternalStockDeliveryResponse();
        r.setId(d.getId());
        r.setReceiptNumber(d.getReceiptNumber());
        r.setDeliveryDate(d.getDeliveryDate());
        if (d.getInventoryItem() != null) {
            r.setInventoryItemId(d.getInventoryItem().getId());
            r.setInventoryItemName(d.getInventoryItem().getItemName());
            r.setInventoryItemSku(d.getInventoryItem().getSku());
        }
        r.setUnit(d.getUnit());
        r.setQuantity(d.getQuantity());
        if (d.getDeliverer() != null) {
            r.setDelivererId(d.getDeliverer().getId());
            r.setDelivererName(d.getDeliverer().getFullName());
        }
        if (d.getReceiver() != null) {
            r.setReceiverId(d.getReceiver().getId());
            r.setReceiverName(d.getReceiver().getFullName());
        }
        r.setSourceLocation(d.getSourceLocation());
        r.setDestinationLocation(d.getDestinationLocation());
        r.setPurpose(d.getPurpose());
        r.setNotes(d.getNotes());
        r.setStatus(d.getStatus());
        if (d.getCreatedBy() != null) {
            r.setCreatedById(d.getCreatedBy().getId());
            r.setCreatedByName(d.getCreatedBy().getFullName());
        }
        if (d.getApprovedBy() != null) {
            r.setApprovedById(d.getApprovedBy().getId());
            r.setApprovedByName(d.getApprovedBy().getFullName());
        }
        if (d.getIssuedBy() != null) {
            r.setIssuedById(d.getIssuedBy().getId());
            r.setIssuedByName(d.getIssuedBy().getFullName());
        }
        if (d.getReceivedBy() != null) {
            r.setReceivedById(d.getReceivedBy().getId());
            r.setReceivedByName(d.getReceivedBy().getFullName());
        }
        r.setStockMovementId(d.getStockMovementId());
        r.setVersion(d.getVersion());
        r.setCreatedAt(d.getCreatedAt());
        r.setUpdatedAt(d.getUpdatedAt());
        return r;
    }
}
