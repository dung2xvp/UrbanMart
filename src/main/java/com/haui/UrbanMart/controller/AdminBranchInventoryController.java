package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryCategoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventorySummaryResponse;
import com.haui.UrbanMart.service.BranchInventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminBranchInventoryController {

    private final BranchInventoryService branchInventoryService;

    @GetMapping("/inventory/summary")
    public ApiResponse<List<BranchInventorySummaryResponse>> getInventorySummaries() {
        return ApiResponse.success(
                "Lấy tổng tồn kho các chi nhánh thành công",
                branchInventoryService.getBranchInventorySummaries()
        );
    }

    @GetMapping("/{branchId}/inventory/by-categories")
    public ApiResponse<BranchInventoryByCategoryResponse> getInventoryByCategories(
            @PathVariable UUID branchId
    ) {
        return ApiResponse.success(
                "Lấy tồn kho theo danh mục chi nhánh thành công",
                branchInventoryService.getInventoryByCategories(branchId)
        );
    }

    @GetMapping("/{branchId}/inventory/summary")
    public ApiResponse<BranchInventoryReportResponse> getInventoryReport(
            @PathVariable UUID branchId
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho chi nhánh thành công",
                branchInventoryService.getInventoryReport(branchId)
        );
    }

    @GetMapping("/{branchId}/inventory/categories")
    public ApiResponse<List<BranchInventoryCategoryReportResponse>> getInventoryCategoryReport(
            @PathVariable UUID branchId
    ) {
        return ApiResponse.success(
                "Lấy báo cáo tồn kho theo danh mục thành công",
                branchInventoryService.getInventoryCategoryReport(branchId)
        );
    }
}
