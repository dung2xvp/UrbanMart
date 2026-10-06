package com.haui.UrbanMart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CategoryCreateRequest {
    @NotBlank
    private String name;

    @Valid
    private List<CategoryCreateRequest> children = new ArrayList<>();
}