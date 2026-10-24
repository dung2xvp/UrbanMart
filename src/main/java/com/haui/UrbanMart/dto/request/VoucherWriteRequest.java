package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record VoucherWriteRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 5000)
        String description,
        @NotNull PromotionType discountType,
        @NotNull @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
        @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2) BigDecimal maxDiscountAmount,
        @NotNull @DecimalMin(value = "0.00")
        @Digits(integer = 10, fraction = 2) BigDecimal minOrderAmount,
        @NotNull OffsetDateTime startDate,
        @NotNull OffsetDateTime endDate,
        UUID branchId,
        @Min(0) Integer usageLimit,
        @NotNull @Positive Integer userLimit,
        @NotNull Boolean isActive,
        @Valid Conditions conditions
) {
    public record Conditions(
            Boolean isFirstOrder,
            Set<@NotNull UUID> applicableCategoryIds,
            Set<@NotNull @Min(1) @Max(31) Integer> specialDaysOfMonth
    ) {
    }
}
