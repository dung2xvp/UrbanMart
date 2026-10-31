package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchNewOrderEvent(
        UUID orderId,
        String orderCode,
        UUID branchId,
        OrderStatus status,
        OffsetDateTime createdAt
) {
}
