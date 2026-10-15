package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchCancelOrderRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
