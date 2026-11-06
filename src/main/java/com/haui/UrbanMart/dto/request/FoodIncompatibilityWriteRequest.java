package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.IncompatibilitySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FoodIncompatibilityWriteRequest(
        @NotBlank @Size(max = 50) String tagA,
        @NotBlank @Size(max = 50) String tagB,
        @NotNull IncompatibilitySeverity severity,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String effect,
        @Size(max = 2000) String recommendation,
        Boolean isActive
) {
}
