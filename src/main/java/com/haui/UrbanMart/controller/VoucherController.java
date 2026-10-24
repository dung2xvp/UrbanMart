package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.request.OrderQuoteRequest;
import com.haui.UrbanMart.dto.request.VoucherApplyRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.PageResponse;
import com.haui.UrbanMart.dto.response.VoucherApplyResponse;
import com.haui.UrbanMart.dto.response.VoucherResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.OrderService;
import com.haui.UrbanMart.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResponse<VoucherResponse>> getVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean isAvailableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePage(page, size);
        return ApiResponse.success(
                "Lấy danh sách voucher thành công",
                PageResponse.from(voucherService.getPublicVouchers(
                        keyword,
                        isAvailableOnly,
                        PageRequest.of(page, size)
                ))
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<VoucherResponse> getVoucher(@PathVariable UUID id) {
        return ApiResponse.success(
                "Lấy chi tiết voucher thành công",
                voucherService.getPublicVoucher(id)
        );
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<VoucherApplyResponse> applyVoucher(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody VoucherApplyRequest request
    ) {
        var quote = orderService.quoteOrder(
                currentUser.getId(),
                new OrderQuoteRequest(request.branchId(), request.code())
        );
        return ApiResponse.success(
                "Áp dụng voucher thành công",
                new VoucherApplyResponse(
                        quote.promotionCode(),
                        quote.discountAmount(),
                        quote.totalAmount()
                )
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoặc size không hợp lệ");
        }
    }
}
