package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.IngredientGroup;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.entity.RecipeCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RecipeAdminResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String imageUrl,
        RecipeCategory categoryMonAn,
        String categoryLabel,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<Ingredient> ingredients
) {
    public record Ingredient(
            UUID id,
            UUID productId,
            String productName,
            String sku,
            String imageUrl,
            String unit,
            /** Gia goc products.base_price - admin khong gan voi chi nhanh nen khong tra gia khuyen mai/ton kho. */
            BigDecimal price,
            ProductStatus productStatus,
            int quantity,
            boolean isRequired,
            IngredientGroup group,
            String note
    ) {
    }
}
