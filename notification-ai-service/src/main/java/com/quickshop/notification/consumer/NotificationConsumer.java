package com.quickshop.notification.consumer;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.notification.config.RabbitMQNotificationConfig;
import com.quickshop.notification.dto.SendEmailRequest;
import com.quickshop.notification.dto.SendNotificationRequest;
import com.quickshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_NOTIFICATION_EMAIL)
    public void handleEmailRequest(SendEmailRequest request) {
        log.info("Received async email dispatch request for: {}", request.getRecipientEmail());
        notificationService.sendEmail(request);
    }

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_ORDER_NOTIFICATION)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId: {}, customerId: {}", event.getOrderId(), event.getCustomerId());

        // 1. Create in-app Notification for Buyer
        if (event.getCustomerId() != null) {
            String title = "Đơn hàng #" + event.getOrderId() + " đã được khởi tạo";
            String content = "Cảm ơn bạn đã mua sắm tại QuickShop! Đơn hàng trị giá " +
                    (event.getTotalAmount() != null ? event.getTotalAmount().toPlainString() : "0") +
                    "đ đã được gửi tới người bán để đóng gói.";

            notificationService.createNotification(SendNotificationRequest.builder()
                    .userId(event.getCustomerId())
                    .title(title)
                    .content(content)
                    .type("ORDER")
                    .build());
        }

        // 2. Send email notification if customerEmail is provided
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
            notificationService.sendEmail(SendEmailRequest.builder()
                    .recipientEmail(event.getCustomerEmail())
                    .subject("[QuickShop] Xác nhận đơn hàng #" + event.getOrderId() + " thành công")
                    .content("Xin chào bạn,\n\nĐơn hàng #" + event.getOrderId() + " của bạn đã được tiếp nhận thành công trên hệ thống QuickShop Marketplace.\nChúng tôi sẽ sớm thông báo khi đơn vị vận chuyển lấy hàng.\n\nTrân trọng,\nĐội ngũ QuickShop.")
                    .build());
        }
    }
}
