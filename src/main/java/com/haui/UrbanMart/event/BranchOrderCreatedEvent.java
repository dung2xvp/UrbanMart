package com.haui.UrbanMart.event;

import java.util.UUID;

public record BranchOrderCreatedEvent(
        UUID accountId,
        UUID orderId,
        String orderCode,
        UUID branchId
) {
}
