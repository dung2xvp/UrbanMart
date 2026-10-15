package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryCategoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryReportResponse;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.BranchInventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branch/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('BRANCH')")
public class BranchInventoryReportController {

    private final BranchInventoryService branchInventoryService;

    @GetMapping("/summary")
    public ApiResponse<BranchInventoryReportResponse> getSummary(
            @RequestParam UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho chi nhánh thành công",
                branchInventoryService.getOwnInventoryReport(branchId, currentUser.getId())
        );
    }

    @GetMapping("/categories")
    public ApiResponse<List<BranchInventoryCategoryReportResponse>> getCategories(
            @RequestParam UUID branchId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho theo danh mục thành công",
                branchInventoryService.getOwnInventoryCategoryReport(
                        branchId, currentUser.getId()
                )
        );
    }
}
