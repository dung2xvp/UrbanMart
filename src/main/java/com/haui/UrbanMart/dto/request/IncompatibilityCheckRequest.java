package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record IncompatibilityCheckRequest(
        @NotEmpty @Size(max = 200) List<@NotNull UUID> productIds
) {
}
