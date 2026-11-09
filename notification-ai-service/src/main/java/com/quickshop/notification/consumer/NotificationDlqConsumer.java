package com.quickshop.notification.consumer;

import com.quickshop.notification.config.RabbitMQNotificationConfig;
import com.quickshop.notification.entity.EmailLog;
import com.quickshop.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDlqConsumer {

    private final EmailLogRepository emailLogRepository;

    @RabbitListener(queues = RabbitMQNotificationConfig.QUEUE_NOTIFICATION_DLQ)
    public void handleDeadLetterMessage(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("Received message in Dead Letter Queue (DLQ): {}", payload);

        try {
            EmailLog dlqLog = EmailLog.builder()
                    .recipientEmail("system-dlq@quickshop.vn")
                    .subject("DEAD_LETTER_EVENT")
                    .templateName("DLQ_MONITORING")
                    .contentPreview(payload.length() > 500 ? payload.substring(0, 500) : payload)
                    .status("FAILED")
                    .errorMessage("Message routing failed and forwarded to DLQ")
                    .retryCount(3)
                    .build();
            emailLogRepository.save(dlqLog);
        } catch (Exception ex) {
            log.error("Failed to record DLQ message into database: {}", ex.getMessage());
        }
    }
}
