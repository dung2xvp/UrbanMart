package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.CreateOrderRequest;
import com.haui.UrbanMart.dto.request.CustomerOrderGroup;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.OrderDetailResponse;
import com.haui.UrbanMart.dto.response.OrderSummaryResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PageResponse<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") CustomerOrderGroup group
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
        return ApiResponse.success(
                "Lấy danh sách đơn hàng thành công",
                PageResponse.from(orderService.getCustomerOrders(
                        currentUser.getId(),
                        group,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                ))
        );
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDetailResponse> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(
                "Lấy chi tiết đơn hàng thành công",
                orderService.getCustomerOrder(currentUser.getId(), orderId)
        );
    }

    @PatchMapping("/{orderId}/confirm-received")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDetailResponse> confirmOrderReceived(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID orderId
    ) {
        return ApiResponse.success(
                "Xác nhận đã nhận hàng thành công",
                orderService.confirmOrderReceived(currentUser.getId(), orderId)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderDetailResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderDetailResponse order = orderService.createOrder(
                currentUser.getId(),
                request
        );

        return ApiResponse.success("Tạo đơn hàng thành công", order);
    }
}