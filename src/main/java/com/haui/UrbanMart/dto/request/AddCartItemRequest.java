package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddCartItemRequest {
    @NotNull
    private UUID branchId;

    @NotNull
    private UUID productId;

    @Min(1)
    private int quantity;
}
