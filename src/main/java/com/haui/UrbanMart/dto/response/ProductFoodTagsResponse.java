package com.haui.UrbanMart.dto.response;

import java.util.List;
import java.util.UUID;

public record ProductFoodTagsResponse(
        UUID productId,
        String productName,
        List<FoodTagResponse> tags
) {
}
