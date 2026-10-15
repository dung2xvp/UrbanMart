package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.BranchInventoryProductResponse;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.BranchInventoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminInventoryController {

    private final BranchInventoryService branchInventoryService;

    @GetMapping("/by-branch")
    public ApiResponse<PageResponse<BranchInventoryProductResponse>> getInventoryByBranch(
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
        return ApiResponse.success(
                "Lấy danh sách tồn kho thành công",
                PageResponse.from(branchInventoryService.searchAdminInventory(
                                branchId,
                                categoryId,
                                keyword,
                                PageRequest.of(page, size)
                        ))
        );
    }
}
