package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.ResendOtpRequest;
import com.haui.UrbanMart.dto.request.UserCreationRequest;
import com.haui.UrbanMart.dto.request.VerifyOtpRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
