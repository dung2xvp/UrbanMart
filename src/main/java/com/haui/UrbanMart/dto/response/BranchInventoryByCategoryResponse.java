package com.haui.UrbanMart.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BranchInventoryByCategoryResponse(
        UUID branchId,
        String branchName,
        String branchAddress,
        long totalBranchStock,
        List<CategoryInventory> categories
) {
    public record CategoryInventory(
            UUID categoryId,
            String categoryName,
            long totalStock,
            List<SubGroup> subGroups
    ) {
    }

    public record SubGroup(
            UUID subCategoryId,
            String parentCategoryName,
            String subCategoryName,
            int totalProducts,
            long totalGroupStock,
            List<ProductInventory> products
    ) {
    }

    public record ProductInventory(
            UUID inventoryId,
            UUID productId,
            String productName,
            String sku,
            String unit,
            int stockQuantity,
            OffsetDateTime updatedAt
    ) {
    }
}
