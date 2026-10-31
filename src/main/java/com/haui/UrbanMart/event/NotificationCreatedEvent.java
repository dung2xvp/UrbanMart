package com.haui.UrbanMart.event;

import java.util.UUID;

public record NotificationCreatedEvent(UUID userId, UUID notificationId) {
}
