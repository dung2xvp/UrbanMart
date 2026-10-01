package com.haui.UrbanMart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CartItemResponse {
    private UUID itemId;
    private UUID productId;
    private String productName;
    private String imageUrl;
    private int quantity;
    private BigDecimal basePrice;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private OffsetDateTime discountStartsAt;
    private OffsetDateTime discountEndsAt;
    private BigDecimal subtotal;
}
