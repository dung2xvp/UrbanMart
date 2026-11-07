package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.RecipeWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.dto.response.RecipeAdminResponse;
import com.haui.UrbanMart.dto.response.RecipeIngredientsResponse;
import com.haui.UrbanMart.dto.response.RecipeSummaryResponse;
import com.haui.UrbanMart.entity.RecipeCategory;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.RecipeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/recipes")
    public ResponseEntity<ApiResponse<RecipeAdminResponse>> createRecipe(
            @Valid @RequestBody RecipeWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Tạo món ăn thành công", recipeService.create(request))
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/recipes")
    public ApiResponse<PageResponse<RecipeAdminResponse>> getAdminRecipes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecipeCategory categoryMonAn,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = pageable(page, size);
        return ApiResponse.success(
                "Lấy danh sách món ăn thành công",
                PageResponse.from(recipeService.getAdminRecipes(
                        keyword, categoryMonAn, isActive, pageable
                ))
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/recipes/{id}")
    public ApiResponse<RecipeAdminResponse> getAdminRecipe(@PathVariable UUID id) {
        return ApiResponse.success("Lấy chi tiết món ăn thành công",
                recipeService.getAdminRecipe(id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/recipes/{id}")
    public ApiResponse<RecipeAdminResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeWriteRequest request
    ) {
        return ApiResponse.success("Cập nhật món ăn thành công",
                recipeService.update(id, request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/v1/admin/recipes/{id}")
    public ApiResponse<Void> deactivateRecipe(@PathVariable UUID id) {
        recipeService.deactivate(id);
        return ApiResponse.success("Ẩn món ăn thành công", null);
    }

    @GetMapping("/api/v1/recipes")
    public ApiResponse<PageResponse<RecipeSummaryResponse>> getRecipes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecipeCategory categoryMonAn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "Lấy danh sách món ăn thành công",
                PageResponse.from(recipeService.getPublicRecipes(
                        keyword,
                        categoryMonAn,
                        pageable(page, size)
                ))
        );
    }

    @GetMapping("/api/v1/recipes/{id}/ingredients")
    public ApiResponse<RecipeIngredientsResponse> getRecipeIngredients(
            @PathVariable UUID id,
            @RequestParam UUID branchId
    ) {
        return ApiResponse.success(
                "Lấy danh sách nguyên liệu thành công",
                recipeService.getIngredientsForBranch(id, branchId)
        );
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}
