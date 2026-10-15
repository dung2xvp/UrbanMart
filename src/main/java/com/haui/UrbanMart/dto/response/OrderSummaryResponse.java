package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.DeliveryType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.PaymentMethod;
import com.haui.UrbanMart.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderCode,
        UUID branchId,
        String branchName,
        DeliveryType deliveryType,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        OffsetDateTime deliveredAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderCode(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getDeliveryType(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getDeliveredAt()
        );
    }
}
