package com.haui.UrbanMart.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BranchInventoryLotResponse(
        UUID lotId,
        UUID productId,
        String productName,
        int quantity,
        LocalDate expiryDate,
        OffsetDateTime receivedAt
) {
}
