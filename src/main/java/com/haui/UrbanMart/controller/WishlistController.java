package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.WishlistProductResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.WishlistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<WishlistProductResponse>> getMyWishlist(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy danh sách sản phẩm yêu thích thành công",
                wishlistService.getMyWishlist(currentUser.getId())
        );
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> add(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID productId
    ) {
        wishlistService.add(currentUser.getId(), productId);
        return ApiResponse.success("Them yeu thich thanh cong", null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID productId
    ) {
        wishlistService.remove(currentUser.getId(), productId);
        return ApiResponse.success("Bo yeu thich thanh cong", null);
    }
}
