package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.AdminUserCreateRequest;
import com.haui.UrbanMart.dto.request.AdminUserUpdateRequest;
import com.haui.UrbanMart.dto.response.AdminUserResponse;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.AdminUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        return ApiResponse.success(
                "Lấy danh sách tài khoản thành công",
                adminUserService.getUsers(
                        keyword,
                        role,
                        status,
                        PageRequest.of(page, size, Sort.by("createdAt").descending())
                )
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(
                "Lấy thông tin tài khoản thành công",
                adminUserService.getUser(userId)
        );
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        return ApiResponse.success(
                "Tạo tài khoản thành công",
                adminUserService.createUser(request)
        );
    }

    @PutMapping("/{userId}")
    public ApiResponse<AdminUserResponse> updateUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật tài khoản thành công",
                adminUserService.updateUser(userId, currentUser.getId(), request)
        );
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<AdminUserResponse> deactivateUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Đã khóa tài khoản",
                adminUserService.deactivateUser(userId, currentUser.getId())
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
    }
}
