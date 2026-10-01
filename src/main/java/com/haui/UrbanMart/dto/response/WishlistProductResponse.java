package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Wishlist;
import com.haui.UrbanMart.service.ProductPrice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class WishlistProductResponse {
    private UUID wishlistId;
    private UUID productId;
    private String sku;
    private String name;
    private String imageUrl;
    private BigDecimal basePrice;
    private BigDecimal displayPrice;
    private BigDecimal discountPercent;
    private OffsetDateTime discountStartsAt;
    private OffsetDateTime discountEndsAt;
    private OffsetDateTime addedAt;

    public static WishlistProductResponse from(
            Wishlist wishlist,
            ProductPrice productPrice
    ) {
        var product = wishlist.getProduct();

        return new WishlistProductResponse(
                wishlist.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getImageUrl(),
                productPrice.basePrice(),
                productPrice.price(),
                productPrice.discountPercent(),
                productPrice.discountStartsAt(),
                productPrice.discountEndsAt(),
                wishlist.getCreatedAt()
        );
    }
}