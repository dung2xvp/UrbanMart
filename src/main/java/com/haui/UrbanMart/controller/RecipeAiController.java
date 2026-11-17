package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.RecipeAiSuggestionRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.RecipeAiSuggestionsResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.AiRateLimitService;
import com.haui.UrbanMart.service.RecipeAiSuggestionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecipeAiController {

    private final RecipeAiSuggestionService suggestionService;
    private final AiRateLimitService rateLimitService;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/ai/recipes/suggestions")
    public ResponseEntity<ApiResponse<RecipeAiSuggestionsResponse>> suggestRecipes(
            @Valid @RequestBody RecipeAiSuggestionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        rateLimitService.checkRecipeSuggestionLimit(user.getId());
        RecipeAiSuggestionsResponse response = suggestionService.suggest(request.query());
        String message = response.suggestions().isEmpty()
                ? "Không tìm thấy công thức phù hợp"
                : "Gợi ý món ăn thành công";
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(message, response));
    }
}
