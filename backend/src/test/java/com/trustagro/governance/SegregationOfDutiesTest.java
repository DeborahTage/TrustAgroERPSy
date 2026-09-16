package com.trustagro.governance;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.CrmQuotation;
import com.trustagro.crm.entity.QuotationStatus;
import com.trustagro.crm.repository.*;
import com.trustagro.crm.service.CrmCommercialService;
import com.trustagro.consultancy.repository.ConsultancyContractRepository;
import com.trustagro.consultancy.repository.ConsultancyEngagementRepository;
import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.service.CrmService;
import com.trustagro.crm.service.CustomerOrderService;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.inventory.entity.InternalRequisition;
import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.inventory.entity.PurchaseOrderStatus;
import com.trustagro.inventory.entity.RequisitionStatus;
import com.trustagro.inventory.repository.InternalRequisitionRepository;
import com.trustagro.inventory.repository.InventoryItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderItemRepository;
import com.trustagro.inventory.repository.PurchaseOrderRepository;
import com.trustagro.inventory.repository.SupplierRepository;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.inventory.service.RequisitionService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.procurement.entity.SupplierInvoice;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;
import com.trustagro.procurement.repository.*;
import com.trustagro.procurement.service.ProcNumberService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.training.repository.TrainingRegistrationRepository;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SegregationOfDutiesTest {

    @Mock ProcNumberService numberService;
    @Mock ProcRequisitionRepository procRequisitionRepository;
    @Mock ProcRequisitionItemRepository requisitionItemRepository;
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderItemRepository purchaseOrderItemRepository;
    @Mock GoodsReceiptRepository goodsReceiptRepository;
    @Mock GoodsReceiptItemRepository goodsReceiptItemRepository;
    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock SupplierInvoiceItemRepository supplierInvoiceItemRepository;
    @Mock SupplierPaymentRepository supplierPaymentRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryService inventoryService;
    @Mock FinanceService financeService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    @InjectMocks ProcurementService procurementService;

    @Mock CrmLeadRepository leadRepository;
    @Mock CrmOpportunityRepository opportunityRepository;
    @Mock CrmQuotationRepository quotationRepository;
    @Mock CrmTaskRepository taskRepository;
    @Mock CrmActivityRepository activityRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock CrmNumberSequenceRepository sequenceRepository;
    @Mock CustomerOrderRepository orderRepository;
    @Mock CrmOrderPaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock ConsultancyEngagementRepository engagementRepository;
    @Mock ConsultancyContractRepository contractRepository;
    @Mock TrainingRegistrationRepository trainingRegistrationRepository;
    @Mock CustomerOrderService orderService;
    @Mock CrmService crmService;
    @Mock ConsultancyService consultancyService;

    @InjectMocks CrmCommercialService commercialService;

    @Mock InternalRequisitionRepository internalRequisitionRepository;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void purchaseOrderSelfApproveRejected() {
        User me = user(1L, "buyer@trustagro.com");
        authenticate(me);
        PurchaseOrder po = new PurchaseOrder();
        po.setId(10L);
        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        po.setCreatedBy(me);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(po));
        when(userRepository.findByEmail(me.getEmail())).thenReturn(Optional.of(me));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> procurementService.approvePurchaseOrder(10L));
        assertTrue(ex.getMessage().toLowerCase().contains("self-approve"));
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void supplierInvoiceSelfApproveRejected() {
        User me = user(2L, "ap@trustagro.com");
        authenticate(me);
        SupplierInvoice inv = new SupplierInvoice();
        inv.setId(11L);
        inv.setStatus(SupplierInvoiceStatus.MATCHED);
        inv.setCreatedBy(me);
        when(supplierInvoiceRepository.findById(11L)).thenReturn(Optional.of(inv));
        when(userRepository.findByEmail(me.getEmail())).thenReturn(Optional.of(me));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> procurementService.approveSupplierInvoice(11L));
        assertTrue(ex.getMessage().toLowerCase().contains("self-approve"));
        verify(supplierInvoiceRepository, never()).save(any());
    }

    @Test
    void quotationSelfApproveRejected() {
        User me = user(3L, "crm@trustagro.com");
        authenticate(me);
        CrmQuotation q = new CrmQuotation();
        q.setId(12L);
        q.setStatus(QuotationStatus.SUBMITTED);
        q.setCreatedBy(me);
        when(quotationRepository.findById(12L)).thenReturn(Optional.of(q));
        when(userRepository.findByEmail(me.getEmail())).thenReturn(Optional.of(me));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> commercialService.approveQuotation(12L));
        assertTrue(ex.getMessage().toLowerCase().contains("self-approve"));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void internalRequisitionSelfApproveRejected() {
        User me = user(4L, "store@trustagro.com");
        InternalRequisition req = new InternalRequisition();
        req.setId(8L);
        req.setStatus(RequisitionStatus.REQUESTED);
        req.setRequestedBy(me);
        when(internalRequisitionRepository.findById(8L)).thenReturn(Optional.of(req));

        RequisitionService service = new RequisitionService(
                internalRequisitionRepository, null, null, null, null, null, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approveRequisition(8L, me));
        assertTrue(ex.getMessage().toLowerCase().contains("self-approve"));
        verify(internalRequisitionRepository, never()).save(any());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(user.getEmail(), "n/a"));
    }

    private static User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }
}
