package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Thay the toan bo tag cua san pham; gui danh sach rong de go het tag. */
public record ProductFoodTagsRequest(
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 50) String> tagCodes
) {
}
