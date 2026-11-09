package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.IngredientGroup;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecipeIngredientsResponse(
        UUID recipeId,
        String recipeName,
        UUID branchId,
        List<Group> groups
) {
    public record Group(
            IngredientGroup group,
            String groupName,
            List<Item> items
    ) {
    }

    public record Item(
            UUID productId,
            String name,
            String imageUrl,
            /** Gia khach tra hien tai (sau khuyen mai). */
            BigDecimal price,
            BigDecimal originalPrice,
            BigDecimal discountPercent,
            String unit,
            int quantity,
            boolean isRequired,
            String note,
            boolean inStock,
            int availableQuantity,
            boolean hasSimilarProducts,
            /** Chi co du lieu khi inStock = false; toi da 3 san pham cung danh muc con hang. */
            List<SimilarProduct> similarProducts
    ) {
    }

    public record SimilarProduct(
            UUID productId,
            String name,
            String imageUrl,
            BigDecimal price,
            BigDecimal originalPrice,
            String unit
    ) {
    }
}
