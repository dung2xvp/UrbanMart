package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderQuoteRequest(
        @NotNull UUID branchId,
        @NotBlank @Size(max = 50) String promotionCode
) {
}
