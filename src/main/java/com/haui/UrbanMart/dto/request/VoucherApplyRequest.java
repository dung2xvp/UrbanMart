package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VoucherApplyRequest(
        @NotBlank @Size(max = 50) String code,
        @NotNull UUID branchId
) {
}
