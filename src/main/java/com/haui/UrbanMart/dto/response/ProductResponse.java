package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.service.ProductPrice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String unit;
    private BigDecimal basePrice;
    private BigDecimal displayPrice;
    private BigDecimal discountPercent;
    private OffsetDateTime discountStartsAt;
    private OffsetDateTime discountEndsAt;
    private boolean dailyFresh;
    private String imageUrl;
    private int stockQuantity;
    private boolean inStock;

    public static ProductResponse from(
            Product product,
            int stockQuantity,
            ProductPrice productPrice
    ) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getUnit(),
                productPrice.basePrice(),
                productPrice.price(),
                productPrice.discountPercent(),
                productPrice.discountStartsAt(),
                productPrice.discountEndsAt(),
                product.isDailyFresh(),
                product.getImageUrl(),
                stockQuantity,
                stockQuantity > 0
        );
    }
}