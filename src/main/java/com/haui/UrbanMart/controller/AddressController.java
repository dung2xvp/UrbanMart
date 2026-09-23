package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.AddressCreationRequest;
import com.haui.UrbanMart.dto.request.AddressUpdateRequest;
import com.haui.UrbanMart.dto.response.AddressResponse;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.AddressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressResponse>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lay danh sach dia chi thanh cong",
                addressService.getMyAddresses(currentUser.getId())
        );
    }

    @GetMapping("/{addressId}")
    public ApiResponse<AddressResponse> getMyAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID addressId
    ) {
        return ApiResponse.success(
                "Lay dia chi thanh cong",
                addressService.getMyAddress(currentUser.getId(), addressId)
        );
    }

    @PostMapping
    public ApiResponse<AddressResponse> createAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AddressCreationRequest request
    ) {
        return ApiResponse.success(
                "Tao dia chi thanh cong",
                addressService.createAddress(currentUser.getId(), request)
        );
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> updateAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressUpdateRequest request
    ) {
        return ApiResponse.success(
                "Cap nhat dia chi thanh cong",
                addressService.updateAddress(
                        currentUser.getId(),
                        addressId,
                        request
                )
        );
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID addressId
    ) {
        addressService.deleteAddress(currentUser.getId(), addressId);
        return ApiResponse.success("Xoa dia chi thanh cong", null);
    }

    @PatchMapping("/{addressId}/default")
    public ApiResponse<AddressResponse> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID addressId
    ) {
        return ApiResponse.success(
                "Dat dia chi mac dinh thanh cong",
                addressService.setDefaultAddress(
                        currentUser.getId(),
                        addressId
                )
        );
    }
}
