package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class ProductDiscountRequest {

    @NotNull(message = "Sản phẩm không được để trống")
    private UUID productId;

    @NotNull(message = "Phần trăm giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Phần trăm giảm giá phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Phần trăm giảm giá không được vượt quá 100")
    private BigDecimal discountPercent;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private OffsetDateTime startsAt;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private OffsetDateTime endsAt;
}
