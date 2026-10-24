package com.haui.UrbanMart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSuggestionResponse(
        UUID id,
        String name,
        String imageUrl,
        BigDecimal displayPrice,
        BigDecimal basePrice,
        String unit,
        String categoryName
) {
    public static ProductSuggestionResponse from(ProductResponse product) {
        return new ProductSuggestionResponse(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getDisplayPrice(),
                product.getBasePrice(),
                product.getUnit(),
                product.getCategoryName()
        );
    }
}
