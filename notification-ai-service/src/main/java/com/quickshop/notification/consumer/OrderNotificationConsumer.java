package com.quickshop.notification.consumer;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.notification.config.RabbitMQNotificationConfig;
import com.quickshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_NOTIFICATION_ORDER_CREATED)
    public void handleOrderCreatedNotification(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for notification. OrderId: {}", event.getOrderId());
        try {
            notificationService.processOrderCreated(event);
            log.info("Successfully processed notification for orderId: {}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to process order notification for orderId: {}, error: {}", event.getOrderId(), ex.getMessage(), ex);
            throw ex; // Re-throw to trigger retry / dead-letter queue routing
        }
    }
}
