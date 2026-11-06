package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminReviewResponse(
        UUID id,
        UUID branchId,
        String branchName,
        String reviewType,
        UUID orderId,
        String orderCode,
        /** PRODUCT: ten san pham; ORDER: ma don hang. */
        String targetName,
        UUID productId,
        UUID customerId,
        String customerName,
        Integer rating,
        Integer deliveryRating,
        Integer serviceRating,
        String comment,
        List<String> images,
        OffsetDateTime createdAt
) {
}
