package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.BranchNearbyResponse;
import com.haui.UrbanMart.service.BranchService;
import lombok.RequiredArgsConstructor;
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
}
