package com.haui.UrbanMart.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haui.UrbanMart.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.haui.UrbanMart.dto.request.UpdateProfileRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


@RestController
@RequestMapping ("/api/users")
@RequiredArgsConstructor 
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse <UserResponse> getMyProfile (@AuthenticationPrincipal CustomUserDetails curentUser) {
        UserResponse response = userService.getProfile(curentUser.getId());
        return new ApiResponse<>("success", "Lấy hồ sơ thành công", response);
    }
    
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(currentUser.getId(), request);
        return new ApiResponse<>("success", "Cập nhật hồ sơ thành công", response);
    }
}
