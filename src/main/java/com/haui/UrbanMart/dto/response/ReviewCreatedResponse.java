package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response sau khi tao danh gia. productId/isVerifiedPurchase chi co voi danh gia san pham.
 */
public record ReviewCreatedResponse(
        UUID reviewId,
        UUID orderId,
        String orderCode,
        UUID productId,
        Boolean isVerifiedPurchase,
        OffsetDateTime createdAt
) {
}
