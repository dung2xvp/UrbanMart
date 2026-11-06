package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BranchReviewPageResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Summary branchSummary
) {
    public record Item(
            UUID id,
            String reviewType,
            UUID orderId,
            String orderCode,
            String customerName,
            /** So dien thoai da che, vd 0987****21. */
            String phone,
            UUID productId,
            String productName,
            /** ORDER: bang serviceRating; PRODUCT: so sao san pham. */
            Integer rating,
            Integer deliveryRating,
            Integer serviceRating,
            String comment,
            List<String> images,
            OffsetDateTime createdAt
    ) {
    }

    public record Summary(
            Double averageOrderRating,
            Double averageProductRating,
            long totalOrderReviews,
            long totalProductReviews
    ) {
    }
}
