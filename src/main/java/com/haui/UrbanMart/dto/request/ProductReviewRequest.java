package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ProductReviewRequest(
        @NotNull UUID orderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String comment,
        @Size(max = 5)
        List<@NotNull @Size(max = 500)
        @Pattern(regexp = "^https?://\\S+$", message = "URL ảnh không hợp lệ") String> images
) {
}
