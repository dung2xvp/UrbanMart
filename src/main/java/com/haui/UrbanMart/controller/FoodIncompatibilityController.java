package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.FoodIncompatibilityWriteRequest;
import com.haui.UrbanMart.dto.request.FoodTagCreateRequest;
import com.haui.UrbanMart.dto.request.IncompatibilityCheckRequest;
import com.haui.UrbanMart.dto.request.ProductFoodTagsRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.FoodIncompatibilityResponse;
import com.haui.UrbanMart.dto.response.FoodTagResponse;
import com.haui.UrbanMart.dto.response.IncompatibilityCheckResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.dto.response.ProductFoodTagsResponse;
import com.haui.UrbanMart.dto.response.ProductIncompatibilityResponse;
import com.haui.UrbanMart.entity.IncompatibilitySeverity;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.FoodIncompatibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
public class FoodIncompatibilityController {

    private final FoodIncompatibilityService foodIncompatibilityService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/food-incompatibilities")
    public ResponseEntity<ApiResponse<FoodIncompatibilityResponse>> createRule(
            @Valid @RequestBody FoodIncompatibilityWriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo quy tắc kỵ nhau thành công",
                foodIncompatibilityService.createRule(request)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/food-incompatibilities")
    public ApiResponse<PageResponse<FoodIncompatibilityResponse>> getRules(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IncompatibilitySeverity severity,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePage(page, size);
        return ApiResponse.success(
                "Lấy danh sách quy tắc kỵ nhau thành công",
                PageResponse.from(foodIncompatibilityService.getRules(
                        search,
                        severity,
                        isActive,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                ))
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/food-incompatibilities/{id}")
    public ApiResponse<FoodIncompatibilityResponse> getRule(@PathVariable UUID id) {
        return ApiResponse.success("Lấy quy tắc kỵ nhau thành công",
                foodIncompatibilityService.getRule(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/food-incompatibilities/{id}")
    public ApiResponse<FoodIncompatibilityResponse> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody FoodIncompatibilityWriteRequest request
    ) {
        return ApiResponse.success("Cập nhật quy tắc kỵ nhau thành công",
                foodIncompatibilityService.updateRule(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/v1/admin/food-incompatibilities/{id}")
    public ApiResponse<UUID> deleteRule(@PathVariable UUID id) {
        return ApiResponse.success(
                "Đã xóa quy tắc kỵ nhau thành công",
                foodIncompatibilityService.deleteRule(id)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/food-tags")
    public ApiResponse<java.util.List<FoodTagResponse>> getTags() {
        return ApiResponse.success("Lấy danh sách tag thực phẩm thành công",
                foodIncompatibilityService.getTags());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/food-tags")
    public ResponseEntity<ApiResponse<FoodTagResponse>> createTag(
            @Valid @RequestBody FoodTagCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo tag thực phẩm thành công",
                foodIncompatibilityService.createTag(request)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/products/{productId}/food-tags")
    public ApiResponse<ProductFoodTagsResponse> setProductTags(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductFoodTagsRequest request
    ) {
        return ApiResponse.success("Cập nhật tag sản phẩm thành công",
                foodIncompatibilityService.setProductTags(productId, request));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/api/v1/customer/cart/check-incompatibilities")
    public ApiResponse<IncompatibilityCheckResponse> checkProducts(
            @Valid @RequestBody IncompatibilityCheckRequest request
    ) {
        return ApiResponse.success(
                "Kiểm tra kết hợp thực phẩm thành công",
                foodIncompatibilityService.checkProducts(request)
        );
    }

    @GetMapping("/api/v1/customer/products/{productId}/incompatibilities")
    public ApiResponse<ProductIncompatibilityResponse> getProductRules(
            @PathVariable UUID productId
    ) {
        return ApiResponse.success(
                "Lấy thông tin thực phẩm có thể không tương thích thành công",
                foodIncompatibilityService.getProductRules(productId)
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
    }
}
