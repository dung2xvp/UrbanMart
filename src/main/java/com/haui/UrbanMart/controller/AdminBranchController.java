package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.BranchWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchAdminResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.BranchAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/admin/branches")
@RequiredArgsConstructor
public class AdminBranchController {

    private final BranchAdminService branchAdminService;

    @GetMapping
    public ApiResponse<Page<BranchAdminResponse>> getBranches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
        return ApiResponse.success(
                "Lấy danh sách chi nhánh thành công",
                branchAdminService.getBranches(PageRequest.of(page, size))
        );
    }

    @GetMapping("/{branchId}")
    public ApiResponse<BranchAdminResponse> getBranch(@PathVariable UUID branchId) {
        return ApiResponse.success(
                "Lấy chi tiết chi nhánh thành công",
                branchAdminService.getBranch(branchId)
        );
    }

    @PostMapping
    public ApiResponse<BranchAdminResponse> createBranch(
            @Valid @RequestBody BranchWriteRequest request
    ) {
        return ApiResponse.success(
                "Tạo chi nhánh thành công",
                branchAdminService.createBranch(request)
        );
    }

    @PutMapping("/{branchId}")
    public ApiResponse<BranchAdminResponse> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchWriteRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật chi nhánh thành công",
                branchAdminService.updateBranch(branchId, request)
        );
    }

    @DeleteMapping("/{branchId}")
    public ApiResponse<Void> closeBranch(@PathVariable UUID branchId) {
        branchAdminService.closeBranch(branchId);
        return ApiResponse.success("Đã đóng chi nhánh, dữ liệu tồn kho được giữ lại", null);
    }
}
