package com.haui.UrbanMart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoryUpdateRequest {
    @NotBlank
    private String name;

    private UUID parentId;
}