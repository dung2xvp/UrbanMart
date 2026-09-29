package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.AddCartItemRequest;
import com.haui.UrbanMart.dto.request.UpdateCartItemRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.CartResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.CartService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam UUID branchId
    ) {
        return ApiResponse.success("Lay gio hang thanh cong",
                cartService.getCart(currentUser.getId(), branchId));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ApiResponse.success("Them san pham vao gio hang thanh cong",
                cartService.addItem(currentUser.getId(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success("Cap nhat gio hang thanh cong",
                cartService.updateItem(currentUser.getId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID itemId
    ) {
        cartService.removeItem(currentUser.getId(), itemId);
        return ApiResponse.success("Xoa san pham khoi gio hang thanh cong", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam UUID branchId
    ) {
        cartService.clearCart(currentUser.getId(), branchId);
        return ApiResponse.success("Xoa gio hang thanh cong", null);
    }
}
