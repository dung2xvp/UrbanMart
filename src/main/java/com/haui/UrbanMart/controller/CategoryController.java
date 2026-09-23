package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.CategoryDto;
import com.haui.UrbanMart.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categories =
                categoryService.getAllCategories();

        return ApiResponse.success(
                "Lấy danh sách danh mục thành công",
                categories
        );
    }
}
