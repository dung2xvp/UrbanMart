package com.haui.UrbanMart.dto.response;

import java.util.UUID;

public record BranchInventoryReportResponse(
        UUID branchId,
        String branchName,
        long totalSkus,
        long totalOnHandQuantity,
        long totalAvailableQuantity,
        long totalReservedQuantity,
        long lowStockCount,
        long outOfStockCount,
        long expiringSoonCount
) {
}
