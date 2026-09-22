package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("UrbanMart backend dang chay", "OK");
    }
}