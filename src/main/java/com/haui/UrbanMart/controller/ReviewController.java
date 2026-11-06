package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.OrderReviewRequest;
import com.haui.UrbanMart.dto.request.ProductReviewRequest;
import com.haui.UrbanMart.dto.request.ReviewType;
import com.haui.UrbanMart.dto.response.AdminReviewResponse;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchReviewPageResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.dto.response.ProductReviewPageResponse;
import com.haui.UrbanMart.dto.response.ReviewCreatedResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/customer/orders/{orderId}/reviews")
    public ResponseEntity<ApiResponse<ReviewCreatedResponse>> createOrderReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đánh giá đơn hàng thành công",
                reviewService.createOrderReview(currentUser.getId(), orderId, request)
        ));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/customer/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewCreatedResponse>> createProductReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Đánh giá sản phẩm thành công",
                reviewService.createProductReview(currentUser.getId(), productId, request)
        ));
    }

    @GetMapping("/api/v1/products/{productId}/reviews")
    public ApiResponse<ProductReviewPageResponse> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePage(page, size);
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException("rating phải nằm trong khoảng từ 1 đến 5");
        }
        return ApiResponse.success(
                "Lấy đánh giá sản phẩm thành công",
                reviewService.getProductReviews(
                        productId,
                        rating,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
        );
    }

    @PreAuthorize("hasRole('BRANCH')")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/branch/reviews")
    public ApiResponse<BranchReviewPageResponse> getBranchReviews(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "ALL") ReviewType type,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageAndRating(page, size, rating);
        return ApiResponse.success(
                "Lấy đánh giá chi nhánh thành công",
                reviewService.getBranchReviews(
                        currentUser.getId(),
                        type,
                        rating,
                        startDate,
                        endDate,
                        PageRequest.of(page, size)
                )
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/admin/reviews")
    public ApiResponse<PageResponse<AdminReviewResponse>> getAdminReviews(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "ALL") ReviewType type,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageAndRating(page, size, rating);
        Pageable pageable = PageRequest.of(
                page, size, Sort.by("createdAt").descending()
        );
        return ApiResponse.success(
                "Lấy đánh giá toàn hệ thống thành công",
                PageResponse.from(reviewService.getAdminReviews(
                        branchId,
                        type,
                        rating,
                        startDate,
                        endDate,
                        keyword,
                        pageable
                ))
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
    }

    private void validatePageAndRating(int page, int size, Integer rating) {
        validatePage(page, size);
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException("rating phải nằm trong khoảng từ 1 đến 5");
        }
    }
}
