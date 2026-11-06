package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.IncompatibilitySeverity;

import java.util.List;
import java.util.UUID;

public record ProductIncompatibilityResponse(
        UUID productId,
        String productName,
        List<String> tags,
        List<Rule> incompatibleRules
) {
    public record Rule(
            UUID id,
            String matchedTag,
            String incompatibleTag,
            String incompatibleTagName,
            IncompatibilitySeverity severity,
            String title,
            String effect,
            String recommendation
    ) {
    }
}
