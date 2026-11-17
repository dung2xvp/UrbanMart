package com.haui.UrbanMart.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeAiSuggestionServiceTest {

    @Test
    void onlyUniqueSuggestionsFromRetrievedRecipesAreAccepted() {
        UUID retrievedId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        List<RecipeAiSuggestionService.ModelSuggestion> result =
                RecipeAiSuggestionService.filterModelSuggestions(
                        List.of(
                                new RecipeAiSuggestionService.ModelSuggestion(
                                        unknownId, "Không thuộc kết quả truy xuất"
                                ),
                                new RecipeAiSuggestionService.ModelSuggestion(
                                        retrievedId, "  Phù hợp với yêu cầu\n"
                                ),
                                new RecipeAiSuggestionService.ModelSuggestion(
                                        retrievedId, "Trùng công thức"
                                ),
                                new RecipeAiSuggestionService.ModelSuggestion(
                                        retrievedId, " "
                                )
                        ),
                        Set.of(retrievedId)
                );

        assertEquals(1, result.size());
        assertEquals(retrievedId, result.getFirst().recipeId());
        assertEquals("Phù hợp với yêu cầu", result.getFirst().reason());
    }

    @Test
    void suggestionsAreLimitedToThree() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();

        List<RecipeAiSuggestionService.ModelSuggestion> result =
                RecipeAiSuggestionService.filterModelSuggestions(
                        List.of(
                                new RecipeAiSuggestionService.ModelSuggestion(first, "Một"),
                                new RecipeAiSuggestionService.ModelSuggestion(second, "Hai"),
                                new RecipeAiSuggestionService.ModelSuggestion(third, "Ba"),
                                new RecipeAiSuggestionService.ModelSuggestion(fourth, "Bốn")
                        ),
                        Set.of(first, second, third, fourth)
                );

        assertEquals(3, result.size());
    }
}
