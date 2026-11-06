package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.IncompatibilitySeverity;

import java.util.List;
import java.util.UUID;

public record IncompatibilityCheckResponse(
        boolean hasConflict,
        List<Warning> warnings
) {
    public record Warning(
            UUID id,
            IncompatibilitySeverity severity,
            String title,
            String effect,
            String recommendation,
            List<ConflictingProduct> conflictingProducts
    ) {
    }

    public record ConflictingProduct(
            UUID id,
            String name,
            String imageUrl,
            String matchedTag
    ) {
    }
}
