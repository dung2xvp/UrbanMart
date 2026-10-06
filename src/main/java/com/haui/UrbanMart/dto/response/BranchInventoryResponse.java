package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.BranchInventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchInventoryResponse(
        UUID inventoryId,
        UUID branchId,
        String branchName,
        UUID productId,
        String sku,
        String productName,
        int stockQuantity,
        OffsetDateTime updatedAt
) {
    public static BranchInventoryResponse from(BranchInventory inventory) {
        return new BranchInventoryResponse(
                inventory.getId(),
                inventory.getBranch().getId(),
                inventory.getBranch().getName(),
                inventory.getProduct().getId(),
                inventory.getProduct().getSku(),
                inventory.getProduct().getName(),
                inventory.getStockQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
