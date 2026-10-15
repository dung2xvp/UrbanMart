package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.DeliveryType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderItem;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.PaymentMethod;
import com.haui.UrbanMart.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderDetailResponse {
    private UUID id;
    private String orderCode;
    private UUID branchId;
    private DeliveryType deliveryType;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime receiveFrom;
    private OffsetDateTime receiveTo;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime completedAt;
    private String promotionCode;
    private List<OrderItemResponse> items;

    public static OrderDetailResponse from(Order order, List<OrderItem> items) {
        return from(order, items, null);
    }

    public static OrderDetailResponse from(
            Order order,
            List<OrderItem> items,
            String promotionCode
    ) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderCode(),
                order.getBranch().getId(),
                order.getDeliveryType(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingAddress(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getNote(),
                order.getCreatedAt(),
                order.getReceiveFrom(),
                order.getReceiveTo(),
                order.getDeliveredAt(),
                order.getCompletedAt(),
                promotionCode,
                items.stream().map(OrderItemResponse::from).toList()
        );
    }
}
