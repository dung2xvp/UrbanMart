package com.haui.UrbanMart.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductPrice(
        BigDecimal basePrice,
        BigDecimal price,
        BigDecimal discountPercent,
        OffsetDateTime discountStartsAt,
        OffsetDateTime discountEndsAt
) {
}
