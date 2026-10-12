package com.haui.UrbanMart.dto.response;

import java.util.UUID;

public record BranchInventorySummaryResponse(
        UUID branchId,
        String branchName,
        String branchAddress,
        String branchPhone,
        long totalStock
) {
}
