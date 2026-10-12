package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.BranchInventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BRANCH')")
public class BranchInventoryController {

    private final BranchInventoryService branchInventoryService;

    @GetMapping("/{branchId}/inventory/by-categories")
    public ApiResponse<BranchInventoryByCategoryResponse> getOwnInventoryByCategories(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy tồn kho theo danh mục chi nhánh thành công",
                branchInventoryService.getOwnInventoryByCategories(
                        branchId,
                        currentUser.getId()
                )
        );
    }

    @PutMapping("/{branchId}/inventory")
    public ApiResponse<BranchInventoryResponse> setOwnStock(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BranchInventoryWriteRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật tồn kho thành công",
                branchInventoryService.setBranchStock(
                        branchId,
                        currentUser.getId(),
                        request
                )
        );
    }
}
