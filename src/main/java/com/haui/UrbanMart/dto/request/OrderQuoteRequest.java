package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.DeliveryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderQuoteRequest(
        @NotNull UUID branchId,
        @Size(max = 50) String promotionCode,
        @NotNull DeliveryType deliveryType,
        UUID addressId,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal shippingLat,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal shippingLng
) {
}
