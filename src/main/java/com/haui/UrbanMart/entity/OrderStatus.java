package com.haui.UrbanMart.entity;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPING,
    READY_FOR_PICKUP,
    DELIVERY_FAILED,
    DELIVERED_PENDING_CONFIRMATION,
    RETURNED,
    COMPLETED,
    CANCELLED
}
