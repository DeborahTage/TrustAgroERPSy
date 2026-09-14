package com.trustagro.crm.service;

import com.trustagro.crm.entity.CustomerOrder;
import com.trustagro.crm.entity.OrderItem;
import com.trustagro.crm.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final com.trustagro.veterinary.service.WithdrawalService withdrawalService;

    public List<CustomerOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public CustomerOrder createOrder(CustomerOrder order) {
        if (order.getItems() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                // Biological Lock Enforcement
                if (item.getFlockId() != null && "MEAT".equalsIgnoreCase(item.getCategory())) {
                    withdrawalService.validateSalesEligibility(item.getFlockId());
                }

                item.setOrder(order);
                BigDecimal sub = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setSubtotal(sub);
                total = total.add(sub);
            }
            order.setTotalAmount(total);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public CustomerOrder updateStatus(Long id, String status) {
        CustomerOrder order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
