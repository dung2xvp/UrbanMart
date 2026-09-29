package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.*;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.LoginResponse;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping ("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping ("/register")
    public ApiResponse<Void> register (@Valid @RequestBody UserCreationRequest request) {
        authService.register(request);
        return ApiResponse.success("Da gui OTP den so dien thoai" + request.getPhone(), null);
    }

    @PostMapping ("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.getPhone());
        return ApiResponse.success("Da gui lai OTP", null);
    }

    @PostMapping ("/verify-otp")
    public ApiResponse<UserResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        UserResponse response = authService.verifyRegisterOtp(request);
        return ApiResponse.success("Dang ky thanh cong!", response);
    }

    @PostMapping ("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ApiResponse.success(
                "Đăng nhập thành công",
                response
        );
    }

    @PostMapping ("/forgot-password")
    public ApiResponse<Void> forgotPassword (
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(
                "Nếu số điện thoại đã đăng ký, OTP sẽ được gửi đến số điện thoại đó",
                null
        );
    }

    @PostMapping ("/reset-password")
    public ApiResponse<Void> resetPassword (
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(
                "Đã đặt lại mật khẩu",
                null
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping ("/change-password")
    public ApiResponse<Void> changePassword (
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.getId(), request);
        return ApiResponse.success(
                "Đổi mật khẩu thành công!",
                null
        );
    }

}
