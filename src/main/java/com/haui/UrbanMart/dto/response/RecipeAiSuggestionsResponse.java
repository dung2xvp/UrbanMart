package com.haui.UrbanMart.dto.response;

import java.util.List;
import java.util.UUID;

public record RecipeAiSuggestionsResponse(
        String query,
        List<Suggestion> suggestions
) {
    public record Suggestion(
            UUID recipeId,
            String name,
            String category,
            String description,
            String imageUrl,
            String reason,
            List<Ingredient> ingredients
    ) {
    }

    public record Ingredient(
            UUID productId,
            String name,
            String unit,
            int quantity,
            boolean required,
            boolean activeProduct,
            String note
    ) {
    }
}
