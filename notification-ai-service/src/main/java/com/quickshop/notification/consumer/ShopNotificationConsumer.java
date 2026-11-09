package com.quickshop.notification.consumer;

import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.notification.config.RabbitMQNotificationConfig;
import com.quickshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopNotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_NOTIFICATION_SHOP_APPROVED)
    public void handleShopApproved(ShopApprovedEvent event) {
        log.info("Received ShopApprovedEvent for shopId: {}, shopName: {}", event.getShopId(), event.getShopName());
        try {
            notificationService.processShopApproved(event);
            log.info("Successfully processed ShopApproved notification for shopId: {}", event.getShopId());
        } catch (Exception ex) {
            log.error("Failed to process ShopApproved notification for shopId: {}, error: {}", event.getShopId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_NOTIFICATION_SHOP_REJECTED)
    public void handleShopRejected(ShopRejectedEvent event) {
        log.info("Received ShopRejectedEvent for shopId: {}, shopName: {}", event.getShopId(), event.getShopName());
        try {
            notificationService.processShopRejected(event);
            log.info("Successfully processed ShopRejected notification for shopId: {}", event.getShopId());
        } catch (Exception ex) {
            log.error("Failed to process ShopRejected notification for shopId: {}, error: {}", event.getShopId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
