package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchInventoryProductResponse(
        UUID productId,
        String sku,
        String productName,
        String imageUrl,
        String unit,
        UUID categoryId,
        String categoryName,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity,
        OffsetDateTime updatedAt
) {
}
