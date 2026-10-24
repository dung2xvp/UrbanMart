package com.haui.UrbanMart.dto.response;

import java.math.BigDecimal;

public record VoucherApplyResponse(
        String code,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {
}
