package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductWriteRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
        String name,

        String description,

        @NotNull(message = "Danh mục không được để trống")
        UUID categoryId,

        UUID brandId,

        @NotBlank(message = "Đơn vị tính không được để trống")
        @Size(max = 20, message = "Đơn vị tính tối đa 20 ký tự")
        String unit,

        @NotNull(message = "Giá gốc không được để trống")
        @DecimalMin(value = "0.00", message = "Giá gốc không được âm")
        @Digits(integer = 10, fraction = 2, message = "Giá gốc tối đa 10 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal basePrice,

        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
        String imageUrl
) {
}
