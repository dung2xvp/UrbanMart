package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record BranchInventoryWriteRequest(
        @NotNull(message = "Sản phẩm không được để trống")
        UUID productId,

        @NotNull(message = "Chi nhánh không được để trống")
        UUID branchId,

        @NotNull(message = "Số lượng tồn kho không được để trống")
        @PositiveOrZero(message = "Số lượng tồn kho không được âm")
        Integer stockQuantity
) {
}
