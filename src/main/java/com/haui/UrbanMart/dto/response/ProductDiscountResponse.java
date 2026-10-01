package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.ProductDiscount;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProductDiscountResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal discountPercent;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ProductDiscountResponse from(ProductDiscount discount) {
        return new ProductDiscountResponse(
                discount.getId(),
                discount.getProduct().getId(),
                discount.getProduct().getName(),
                discount.getDiscountPercent(),
                discount.getStartsAt(),
                discount.getEndsAt(),
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }
}
