package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.RecipeCategory;

import java.util.UUID;

public record RecipeSummaryResponse(
        UUID id,
        String name,
        String slug,
        String imageUrl,
        RecipeCategory categoryMonAn,
        String categoryLabel,
        long totalIngredients
) {
}
