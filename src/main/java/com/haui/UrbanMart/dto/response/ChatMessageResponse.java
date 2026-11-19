package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.dto.ChatMode;
import com.haui.UrbanMart.entity.PromotionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ChatMessageResponse(
        String messageId,
        String sessionId,
        ChatMode mode,
        String sender,
        String senderName,
        String replyText,
        String type,
        List<SuggestedProduct> suggestedProducts,
        List<SuggestedVoucher> suggestedVouchers,
        List<String> quickReplies,
        OffsetDateTime timestamp
) {
    public record SuggestedProduct(
            UUID id,
            String sku,
            String name,
            BigDecimal basePrice,
            BigDecimal displayPrice,
            String imageUrl,
            String unit,
            int availableQuantity,
            boolean inStock
    ) {
    }

    public record SuggestedVoucher(
            UUID id,
            String code,
            String title,
            PromotionType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            OffsetDateTime endDate,
            boolean canApply,
            BigDecimal amountToMinOrder,
            Set<UUID> applicableCategoryIds
    ) {
    }
}
