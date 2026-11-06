package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.FoodIncompatibility;
import com.haui.UrbanMart.entity.IncompatibilitySeverity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FoodIncompatibilityResponse(
        UUID id,
        String tagA,
        String tagB,
        IncompatibilitySeverity severity,
        String title,
        String effect,
        String recommendation,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static FoodIncompatibilityResponse from(FoodIncompatibility rule) {
        return new FoodIncompatibilityResponse(
                rule.getId(),
                rule.getTagA(),
                rule.getTagB(),
                rule.getSeverity(),
                rule.getTitle(),
                rule.getEffect(),
                rule.getRecommendation(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
