package com.haui.UrbanMart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CartResponse {
    private UUID cartId;
    private UUID branchId;
    private List<CartItemResponse> items;
    private int totalQuantity;
    private BigDecimal totalAmount;
}
