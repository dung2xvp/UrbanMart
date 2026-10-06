package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.ProductWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.ProductAdminResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductAdminResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }

        return ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                productService.getAdminProducts(PageRequest.of(page, size))
        );
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductAdminResponse> getProduct(@PathVariable UUID productId) {
        return ApiResponse.success(
                "Lấy chi tiết sản phẩm thành công",
                productService.getAdminProduct(productId)
        );
    }

    @PostMapping
    public ApiResponse<ProductAdminResponse> createProduct(
            @Valid @RequestBody ProductWriteRequest request
    ) {
        return ApiResponse.success(
                "Tạo sản phẩm thành công",
                productService.createProduct(request)
        );
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductAdminResponse> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductWriteRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật sản phẩm thành công",
                productService.updateProduct(productId, request)
        );
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> discontinueProduct(@PathVariable UUID productId) {
        productService.discontinueProduct(productId);
        return ApiResponse.success("Đã ngừng kinh doanh sản phẩm", null);
    }
}
