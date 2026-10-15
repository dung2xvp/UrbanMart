package com.haui.UrbanMart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPricingResponse(
        UUID branchId,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String promotionCode
) {
}
