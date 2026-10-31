package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.BranchNewOrderEvent;
import com.haui.UrbanMart.event.BranchOrderCreatedEvent;
import com.haui.UrbanMart.event.NotificationCreatedEvent;
import com.haui.UrbanMart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationRealtimeListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendNotification(NotificationCreatedEvent event) {
        notificationService.getNotificationResponse(event.userId(), event.notificationId())
                .ifPresent(notification -> messagingTemplate.convertAndSendToUser(
                        event.userId().toString(),
                        "/queue/notifications",
                        notification
                ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendNewBranchOrder(BranchOrderCreatedEvent event) {
        orderRepository.findByIdAndBranch_Id(event.orderId(), event.branchId())
                .ifPresent(order -> messagingTemplate.convertAndSendToUser(
                        event.accountId().toString(),
                        "/queue/branch-orders",
                        new BranchNewOrderEvent(
                                order.getId(),
                                event.orderCode(),
                                event.branchId(),
                                order.getStatus(),
                                order.getCreatedAt()
                        )
                ));
    }
}
