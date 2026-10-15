package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.DeliveryFailedRequest;
import com.haui.UrbanMart.dto.request.BranchCancelOrderRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.OrderDetailResponse;
import com.haui.UrbanMart.dto.response.OrderSummaryResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.BranchOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/branches/{branchId}/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BRANCH')")
public class BranchOrderController {
    private final BranchOrderService branchOrderService;

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
        return ApiResponse.success(
                "Lấy danh sách đơn hàng chi nhánh thành công",
                PageResponse.from(branchOrderService.getOrders(
                        branchId,
                        currentUser.getId(),
                        status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                ))
        );
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy chi tiết đơn hàng thành công",
                branchOrderService.getOrder(branchId, currentUser.getId(), orderId)
        );
    }

    @PatchMapping("/{orderId}/confirm")
    public ApiResponse<OrderDetailResponse> confirmOrder(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Xác nhận đơn hàng thành công",
                branchOrderService.confirmOrder(branchId, currentUser.getId(), orderId)
        );
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderDetailResponse> cancelOrder(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BranchCancelOrderRequest request
    ) {
        return ApiResponse.success(
                "Hủy đơn hàng thành công",
                branchOrderService.cancelOrder(
                        branchId, currentUser.getId(), orderId, request
                )
        );
    }

    @PatchMapping("/{orderId}/preparation-complete")
    public ApiResponse<OrderDetailResponse> completePreparation(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Hoàn tất chuẩn bị đơn hàng",
                branchOrderService.completePreparation(
                        branchId, currentUser.getId(), orderId
                )
        );
    }

    @PatchMapping("/{orderId}/handover")
    public ApiResponse<OrderDetailResponse> handOverOrder(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Ghi nhận bàn giao đơn hàng thành công",
                branchOrderService.handOverOrder(branchId, currentUser.getId(), orderId)
        );
    }

    @PatchMapping("/{orderId}/delivery-failed")
    public ApiResponse<OrderDetailResponse> markDeliveryFailed(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody DeliveryFailedRequest request
    ) {
        return ApiResponse.success(
                "Ghi nhận giao hàng thất bại",
                branchOrderService.markDeliveryFailed(
                        branchId, currentUser.getId(), orderId, request
                )
        );
    }

    @PatchMapping("/{orderId}/retry-delivery")
    public ApiResponse<OrderDetailResponse> retryDelivery(
            @PathVariable UUID branchId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Đã chuyển đơn sang trạng thái giao lại",
                branchOrderService.retryDelivery(branchId, currentUser.getId(), orderId)
        );
    }
}
