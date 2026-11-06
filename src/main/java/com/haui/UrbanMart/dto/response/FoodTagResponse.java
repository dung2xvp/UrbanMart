package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.FoodTag;

public record FoodTagResponse(
        String code,
        String name
) {
    public static FoodTagResponse from(FoodTag tag) {
        return new FoodTagResponse(tag.getCode(), tag.getName());
    }
}
