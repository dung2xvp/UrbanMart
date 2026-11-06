package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductReviewPageResponse(
        List<Item> content,
        /** Thong ke tren toan bo danh gia cua san pham, khong phu thuoc bo loc rating. */
        double averageRating,
        long totalReviews,
        Map<String, Long> ratingSpreads,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record Item(
            UUID id,
            String customerName,
            String customerAvatar,
            int rating,
            String comment,
            List<String> images,
            boolean isVerifiedPurchase,
            OffsetDateTime createdAt,
            String branchName
    ) {
    }
}
