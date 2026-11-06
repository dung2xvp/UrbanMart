package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FoodTagCreateRequest(
        @NotBlank @Size(max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9_]+$",
                message = "Mã tag chỉ gồm chữ không dấu, số và dấu gạch dưới"
        )
        String code,
        @NotBlank @Size(max = 100) String name
) {
}
