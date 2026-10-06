package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        String sku,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String unit,
        BigDecimal basePrice,
        String imageUrl,
        String status
) {
    public static ProductAdminResponse from(Product product) {
        return new ProductAdminResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getUnit(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getStatus().name()
        );
    }
}
