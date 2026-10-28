package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record VoucherResponse(
        UUID id,
        String code,
        String title,
        String description,
        PromotionType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        UUID branchId,
        Integer usageLimit,
        int usedCount,
        int userLimit,
        boolean isActive,
        Conditions conditions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Boolean usedByCurrentUser,
        Integer userUsageCount,
        VoucherStatus status,
        Boolean canApply,
        List<VoucherIneligibilityReason> ineligibilityReasons
) {
    public static VoucherResponse from(Promotion promotion) {
        return from(promotion, null);
    }

    public static VoucherResponse from(
            Promotion promotion,
            CustomerEligibility eligibility
    ) {
        Set<UUID> categoryIds = promotion.getApplicableCategories().stream()
                .map(category -> category.getId())
                .collect(Collectors.toUnmodifiableSet());
        Conditions conditions = new Conditions(
                promotion.isFirstOrder(),
                categoryIds,
                Set.copyOf(promotion.getSpecialDaysOfMonth())
        );
        return new VoucherResponse(
                promotion.getId(),
                promotion.getCode(),
                promotion.getTitle(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getDiscountValue(),
                promotion.getMaxDiscountAmount(),
                promotion.getMinOrderValue(),
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                promotion.getBranch() == null ? null : promotion.getBranch().getId(),
                promotion.getUsageLimit(),
                promotion.getUsedCount(),
                promotion.getPerUserLimit(),
                promotion.isActive(),
                conditions,
                promotion.getCreatedAt(),
                promotion.getUpdatedAt(),
                eligibility == null ? null : eligibility.usedByCurrentUser(),
                eligibility == null ? null : eligibility.userUsageCount(),
                eligibility == null ? null : eligibility.status(),
                eligibility == null ? null : eligibility.canApply(),
                eligibility == null ? null : eligibility.ineligibilityReasons()
        );
    }

    public record CustomerEligibility(
            boolean usedByCurrentUser,
            int userUsageCount,
            VoucherStatus status,
            boolean canApply,
            List<VoucherIneligibilityReason> ineligibilityReasons
    ) {
    }

    public record Conditions(
            boolean isFirstOrder,
            Set<UUID> applicableCategoryIds,
            Set<Integer> specialDaysOfMonth
    ) {
    }
}
