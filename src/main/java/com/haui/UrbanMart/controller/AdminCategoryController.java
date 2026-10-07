package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.CategoryCreateRequest;
import com.haui.UrbanMart.dto.request.CategoryUpdateRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.CategoryDto;
import com.haui.UrbanMart.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping ("/api/admin/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryDto> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.success(
                "Tạo danh mục thành công",
                categoryService.createCategoryTree(request)
        );
    }

    @PutMapping ("/{id}")
    public ApiResponse<CategoryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.success(
                "Cập nhật danh mục thành công",
                categoryService.updateCategory(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success("Xóa danh mục thành công", null);
    }
}
