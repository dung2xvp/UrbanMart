package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Notification;
import com.haui.UrbanMart.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String content,
        NotificationType type,
        boolean isRead,
        String imageUrl,
        String targetUrl,
        String referenceId,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.isRead(),
                notification.getImageUrl(),
                notification.getTargetUrl(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}
