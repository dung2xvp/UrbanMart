package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateBranchInventoryLotExpiryRequest(
        @NotNull(message = "Ngày hết hạn không được để trống")
        LocalDate expiryDate
) {
}
