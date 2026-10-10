package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchAdminResponse;
import com.haui.UrbanMart.dto.response.BranchNearbyResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.BranchAdminService;
import com.haui.UrbanMart.service.BranchService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/nearby")
    public ApiResponse<List<BranchNearbyResponse>> findNearby(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "20") BigDecimal radiusKm
    ) {
        return ApiResponse.success(
                "Tim chi nhanh gan thanh cong",
                branchService.findNearby(lat, lng, radiusKm)
        );
    }
    private final BranchAdminService branchAdminService;

    @SecurityRequirement(name = "bearerAuth")
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
}
