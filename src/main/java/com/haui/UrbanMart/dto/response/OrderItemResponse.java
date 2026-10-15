package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private UUID productId;
    private String productName;
    private String sku;
    private String imageUrl;
    private String unit;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductName(),
                item.getSku(),
                item.getImageUrl(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
