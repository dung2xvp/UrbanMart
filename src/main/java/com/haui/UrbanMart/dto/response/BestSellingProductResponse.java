package com.haui.UrbanMart.dto.response;

public record BestSellingProductResponse(
        ProductResponse product,
        long quantitySold
) {
}
