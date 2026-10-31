package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.NotificationPageResponse;
import com.haui.UrbanMart.dto.response.NotificationResponse;
import com.haui.UrbanMart.dto.response.UnreadCountResponse;
import com.haui.UrbanMart.entity.Notification;
import com.haui.UrbanMart.entity.NotificationType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.event.NotificationCreatedEvent;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            UUID userId,
            NotificationType type,
            Boolean isRead,
            Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationRepository
                .findForUser(userId, type, isRead, pageable)
                .map(NotificationResponse::from);
        return new NotificationPageResponse(
                notifications.getContent(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notificationRepository.countUnreadByUserId(userId)
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        return new UnreadCountResponse(
                notificationRepository.countUnreadByUserId(userId)
        );
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void delete(UUID userId, UUID notificationId) {
        notificationRepository.delete(findOwnedNotification(userId, notificationId));
    }

    @Transactional
    public void createOrderNotification(Order order, String title, String content) {
        Notification notification = new Notification();
        notification.setUser(order.getCustomer());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(NotificationType.ORDER);
        notification.setTargetUrl("/orders/" + order.getOrderCode());
        notification.setReferenceId(order.getOrderCode());
        notification = notificationRepository.save(notification);
        eventPublisher.publishEvent(
                new NotificationCreatedEvent(
                        order.getCustomer().getId(),
                        notification.getId()
                )
        );
    }

    @Transactional(readOnly = true)
    public Optional<NotificationResponse> getNotificationResponse(
            UUID userId,
            UUID notificationId
    ) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .map(NotificationResponse::from);
    }

    private Notification findOwnedNotification(UUID userId, UUID notificationId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông báo"
                ));
    }
}
