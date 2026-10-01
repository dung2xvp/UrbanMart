package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.ProductDiscountRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.ProductDiscountResponse;
import com.haui.UrbanMart.service.ProductDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/product-discounts")
@RequiredArgsConstructor
public class ProductDiscountController {

    private final ProductDiscountService productDiscountService;

    @GetMapping
    public ApiResponse<List<ProductDiscountResponse>> getAll() {
        return ApiResponse.success(
                "Lấy danh sách đợt giảm giá thành công",
                productDiscountService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDiscountResponse> getById(
            @PathVariable UUID id
    ) {
        return ApiResponse.success(
                "Lấy đợt giảm giá thành công",
                productDiscountService.getById(id)
        );
    }

    @PostMapping
    public ApiResponse<ProductDiscountResponse> create(
            @Valid @RequestBody ProductDiscountRequest request
    ) {
        return ApiResponse.success(
                "Tạo đợt giảm giá thành công",
                productDiscountService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDiscountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductDiscountRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật đợt giảm giá thành công",
                productDiscountService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        productDiscountService.delete(id);
        return ApiResponse.success("Xóa đợt giảm giá thành công", null);
    }
}
