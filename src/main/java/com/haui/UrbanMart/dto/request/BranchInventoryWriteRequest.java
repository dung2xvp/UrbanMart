package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record BranchInventoryWriteRequest(
        @NotNull(message = "Sản phẩm không được để trống")
        UUID productId,

        @NotNull(message = "Số lượng tồn kho không được để trống")
        @PositiveOrZero(message = "Số lượng tồn kho không được âm")
        Integer stockQuantity,

        @NotNull(message = "Lý do giảm tồn kho không được để trống")
        @Pattern(
                regexp = "EXPIRED|DAMAGED",
                message = "Lý do giảm tồn kho chỉ được là EXPIRED hoặc DAMAGED"
        )
        String reason
) {
}
