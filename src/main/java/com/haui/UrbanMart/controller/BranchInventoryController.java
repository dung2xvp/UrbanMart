package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.BranchInventoryLotRequest;
import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.request.UpdateBranchInventoryLotExpiryRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryCategoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryLotResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryReportResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BRANCH')")
public class BranchInventoryController {

    private final BranchInventoryService branchInventoryService;

    @GetMapping("/{branchId}/inventory/summary")
    public ApiResponse<BranchInventoryReportResponse> getOwnInventorySummary(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho chi nhánh thành công",
                branchInventoryService.getOwnInventoryReport(branchId, currentUser.getId())
        );
    }

    @GetMapping("/{branchId}/inventory/categories")
    public ApiResponse<List<BranchInventoryCategoryReportResponse>> getOwnInventoryCategoryReport(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho theo danh mục thành công",
                branchInventoryService.getOwnInventoryCategoryReport(
                        branchId, currentUser.getId()
                )
        );
    }

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

    @PostMapping("/{branchId}/inventory/lots")
    public ApiResponse<BranchInventoryLotResponse> receiveInventoryLot(
            @PathVariable UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody BranchInventoryLotRequest request
    ) {
        return ApiResponse.success(
                "Nhập lô tồn kho thành công",
                branchInventoryService.receiveOwnLot(
                        branchId, currentUser.getId(), request
                )
        );
    }

    @GetMapping("/{branchId}/inventory/{productId}/lots")
    public ApiResponse<List<BranchInventoryLotResponse>> getOwnInventoryLots(
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy danh sách lô tồn kho thành công",
                branchInventoryService.getOwnLots(
                        branchId, currentUser.getId(), productId
                )
        );
    }

    @PatchMapping("/{branchId}/inventory/lots/{lotId}")
    public ApiResponse<BranchInventoryLotResponse> updateOwnLotExpiry(
            @PathVariable UUID branchId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateBranchInventoryLotExpiryRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật ngày hết hạn lô hàng thành công",
                branchInventoryService.updateOwnLotExpiry(
                        branchId, currentUser.getId(), lotId, request
                )
        );
    }
}
