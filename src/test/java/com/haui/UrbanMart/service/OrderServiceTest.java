package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.DeliveryType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderItem;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.repository.OrderItemRepository;
import com.haui.UrbanMart.repository.OrderPromotionRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Test
    void customerCanConfirmPickupWhenOrderIsReady() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setDeliveryType(DeliveryType.PICKUP);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        Branch branch = new Branch();
        branch.setId(branchId);
        order.setBranch(branch);

        Product product = new Product();
        product.setId(productId);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        OrderPromotionRepository orderPromotionRepository =
                mock(OrderPromotionRepository.class);
        InventoryService inventoryService = mock(InventoryService.class);
        when(orderRepository.findByIdAndCustomer_Id(orderId, customerId))
                .thenReturn(Optional.of(order));
        when(orderRepository.confirmPickupOrder(orderId, customerId)).thenAnswer(invocation -> {
            order.setStatus(OrderStatus.COMPLETED);
            return 1;
        });
        when(orderItemRepository.findAllByOrder_IdOrderByProduct_IdAsc(orderId))
                .thenReturn(List.of(item));
        when(orderPromotionRepository.findFirstByOrder_IdOrderByCreatedAtAsc(orderId))
                .thenReturn(Optional.empty());

        OrderService orderService = new OrderService(
                null,
                null,
                null,
                null,
                orderRepository,
                orderItemRepository,
                orderPromotionRepository,
                null,
                null,
                inventoryService,
                null,
                null,
                null
        );

        var result = orderService.confirmOrderReceived(customerId, orderId);

        assertEquals(orderId, result.getId());
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        verify(orderRepository).confirmPickupOrder(orderId, customerId);
        verify(inventoryService).completePickup(
                branchId, productId, 2, order, customerId
        );
    }
}
