package com.haui.UrbanMart.dto.response;

import java.util.List;
import java.util.UUID;

public record BranchInventoryCategoryReportResponse(
        UUID categoryId,
        String categoryName,
        long totalSkus,
        long totalOnHand,
        long totalAvailable,
        long lowStockSkus,
        List<SubCategory> subCategories
) {
    public record SubCategory(
            UUID categoryId,
            String categoryName,
            long totalSkus,
            long totalOnHand,
            long totalAvailable
    ) {
    }
}
