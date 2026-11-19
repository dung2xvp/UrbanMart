package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.dto.ChatMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChatRequest(
        @NotNull ChatMode mode,
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9_-]+")
        String sessionId,
        @NotBlank @Size(max = 2000) String message,
        @NotNull UUID branchId,
        @Valid Context context,
        @Size(max = 5) List<JsonNode> attachments
) {
    public record Context(
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2)
            BigDecimal currentCartTotal,
            UUID categoryId
    ) {
    }

}