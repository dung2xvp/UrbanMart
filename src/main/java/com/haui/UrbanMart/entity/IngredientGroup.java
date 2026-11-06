package com.haui.UrbanMart.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IngredientGroup {
    MAIN("Nguyên liệu chính"),
    SIDE("Nguyên liệu phụ"),
    SPICE("Gia vị");

    private final String label;
}
