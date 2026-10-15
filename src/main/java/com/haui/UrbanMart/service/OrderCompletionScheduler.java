package com.haui.UrbanMart.service;

import com.haui.UrbanMart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionScheduler {
    private final OrderRepository orderRepository;

    @Scheduled(fixedDelayString = "${orders.auto-complete.poll-interval-ms:60000}")
    @Transactional
    public void completeOrdersPastConfirmationDeadline() {
        int completedCount = orderRepository.completeOrdersPastConfirmationDeadline();
        if (completedCount > 0) {
            log.info("Automatically completed {} delivered orders after the confirmation window",
                    completedCount);
        }
    }
}
