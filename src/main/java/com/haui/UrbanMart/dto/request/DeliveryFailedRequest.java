package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryFailedRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
