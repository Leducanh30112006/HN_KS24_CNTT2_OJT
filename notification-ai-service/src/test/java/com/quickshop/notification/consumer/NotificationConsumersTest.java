package com.quickshop.notification.consumer;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.repository.EmailLogRepository;
import com.quickshop.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Consumers - Kiểm thử tiếp nhận message RabbitMQ và Dead Letter Queue")
class NotificationConsumersTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailLogRepository emailLogRepository;

    @InjectMocks
    private OrderNotificationConsumer orderNotificationConsumer;

    @InjectMocks
    private ShopNotificationConsumer shopNotificationConsumer;

    @InjectMocks
    private NotificationDlqConsumer notificationDlqConsumer;

    @Test
    @DisplayName("OrderNotificationConsumer - Tiêu thụ OrderCreatedEvent thành công")
    void orderNotificationConsumer_Success() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ORD-999")
                .totalAmount(BigDecimal.valueOf(100000))
                .build();

        orderNotificationConsumer.handleOrderCreatedNotification(event);

        verify(notificationService, times(1)).processOrderCreated(event);
    }

    @Test
    @DisplayName("ShopNotificationConsumer - Tiêu thụ ShopApprovedEvent thành công")
    void shopNotificationConsumer_ShopApproved_Success() {
        ShopApprovedEvent event = ShopApprovedEvent.builder()
                .shopId(10L)
                .shopName("OfficialStore")
                .build();

        shopNotificationConsumer.handleShopApproved(event);

        verify(notificationService, times(1)).processShopApproved(event);
    }

    @Test
    @DisplayName("ShopNotificationConsumer - Tiêu thụ ShopRejectedEvent thành công")
    void shopNotificationConsumer_ShopRejected_Success() {
        ShopRejectedEvent event = ShopRejectedEvent.builder()
                .shopId(10L)
                .shopName("OfficialStore")
                .reason("Thiếu giấy tờ")
                .build();

        shopNotificationConsumer.handleShopRejected(event);

        verify(notificationService, times(1)).processShopRejected(event);
    }

    @Test
    @DisplayName("NotificationDlqConsumer - Tiếp nhận tin nhắn lỗi từ Dead Letter Queue")
    void dlqConsumer_Success() {
        Message message = new Message("{\"error\":\"routing failed\"}".getBytes(StandardCharsets.UTF_8));

        notificationDlqConsumer.handleDeadLetterMessage(message);

        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
    }
}
