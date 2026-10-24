package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.VoucherWriteRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.dto.response.VoucherResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ApiResponse<PageResponse<VoucherResponse>> getVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePage(page, size);
        return ApiResponse.success(
                "Lấy danh sách voucher thành công",
                PageResponse.from(voucherService.getAdminVouchers(
                        keyword,
                        isActive,
                        PageRequest.of(page, size)
                ))
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> getVoucher(@PathVariable UUID id) {
        return ApiResponse.success(
                "Lấy chi tiết voucher thành công",
                voucherService.getAdminVoucher(id)
        );
    }

    @PostMapping
    public ApiResponse<VoucherResponse> createVoucher(
            @Valid @RequestBody VoucherWriteRequest request
    ) {
        return ApiResponse.success(
                "Tạo voucher thành công",
                voucherService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<VoucherResponse> updateVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherWriteRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật voucher thành công",
                voucherService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivateVoucher(@PathVariable UUID id) {
        voucherService.deactivate(id);
        return ApiResponse.success("Đã vô hiệu hóa voucher", null);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
    }
}
