package com.haui.UrbanMart.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecipeCategory {
    VEGETABLE("Món rau, củ"),
    MEAT("Món thịt"),
    SEAFOOD("Món hải sản"),
    SOUP("Món canh"),
    HOTPOT("Món lẩu"),
    VEGETARIAN("Món chay"),
    DESSERT("Tráng miệng"),
    OTHER("Món khác");

    private final String label;
}
