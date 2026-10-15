package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record BranchInventoryLotRequest(
        @NotNull(message = "Sản phẩm không được để trống")
        UUID productId,

        @NotNull(message = "Số lượng nhập không được để trống")
        @Positive(message = "Số lượng nhập phải lớn hơn 0")
        Integer quantity,

        @FutureOrPresent(message = "Ngày hết hạn không được ở quá khứ")
        LocalDate expiryDate
) {
}
