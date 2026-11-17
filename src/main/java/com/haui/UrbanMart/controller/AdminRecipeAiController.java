package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.service.RecipeKnowledgeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminRecipeAiController {

    private final RecipeKnowledgeService knowledgeService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/ai/recipes/reindex")
    public ApiResponse<ReindexResult> reindexRecipes() {
        int indexed = knowledgeService.reindexAllActiveRecipes();
        return ApiResponse.success(
                "Đã lập chỉ mục lại công thức",
                new ReindexResult(indexed)
        );
    }

    public record ReindexResult(int indexedRecipes) {
    }
}
