package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.service.BranchInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class AdminBranchInventoryController {

    private final BranchInventoryService branchInventoryService;

    @PostMapping("/inventory")
    public ApiResponse<BranchInventoryResponse> setStock(
            @Valid @RequestBody BranchInventoryWriteRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật tồn kho thành công",
                branchInventoryService.setStock(request)
        );
    }

    @GetMapping("/{branchId}/inventory")
    public ApiResponse<List<BranchInventoryResponse>> getBranchInventory(
            @PathVariable UUID branchId
    ) {
        return ApiResponse.success(
                "Lấy tồn kho chi nhánh thành công",
                branchInventoryService.getBranchInventory(branchId)
        );
    }
}
