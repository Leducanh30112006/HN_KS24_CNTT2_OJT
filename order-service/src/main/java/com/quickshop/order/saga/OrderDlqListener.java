package com.quickshop.order.saga;

import com.quickshop.common.event.OrderDlqMessage;
import com.quickshop.order.config.RabbitMQOrderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Dead-Letter Queue (DLQ) Listener giám sát hàng đợi order.dlq:
 * - Tiếp nhận các thông điệp sự kiện thất bại sau tối đa 3 lần retry từ RepublishMessageRecoverer
 * - Trích xuất chi tiết exception, original exchange, routing key, payload
 * - Ghi log cảnh báo mức CRITICAL và lưu trữ phục vụ truy vết / đối soát qua REST API
 */
@Slf4j
@Component
public class OrderDlqListener {

    private static final int MAX_BUFFER_SIZE = 200;
    private final ConcurrentLinkedDeque<OrderDlqMessage> dlqBuffer = new ConcurrentLinkedDeque<>();

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_ORDER_DLQ)
    public void onDeadLetterMessage(Message message) {
        try {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();

            String originalExchange = getHeaderString(headers, "x-original-exchange");
            String originalRoutingKey = getHeaderString(headers, "x-original-routingKey");
            String errorMessage = getHeaderString(headers, "x-exception-message");
            String stackTrace = getHeaderString(headers, "x-exception-stacktrace");

            String payload = message.getBody() != null
                    ? new String(message.getBody(), StandardCharsets.UTF_8)
                    : "";

            OrderDlqMessage dlqMessage = OrderDlqMessage.builder()
                    .originalExchange(originalExchange != null ? originalExchange : RabbitMQOrderConfig.EXCHANGE_ORDER)
                    .originalRoutingKey(originalRoutingKey != null ? originalRoutingKey : "unknown")
                    .payload(payload)
                    .errorMessage(errorMessage != null ? errorMessage : "Không có thông tin chi tiết lỗi")
                    .exceptionClass(extractExceptionClass(stackTrace))
                    .retryCount(3) // Tối đa 3 lần retry
                    .failedAt(LocalDateTime.now())
                    .build();

            recordMessage(dlqMessage);

            log.error("[CRITICAL ALERT] Sự kiện gửi vào Dead Letter Queue (order.dlq)! Original Exchange: {}, RoutingKey: {}, Error: {}, Payload: {}",
                    dlqMessage.getOriginalExchange(),
                    dlqMessage.getOriginalRoutingKey(),
                    dlqMessage.getErrorMessage(),
                    dlqMessage.getPayload());

        } catch (Exception ex) {
            log.error("Lỗi khi xử lý message trong Dead Letter Queue (order.dlq): {}", ex.getMessage(), ex);
        }
    }

    public synchronized void recordMessage(OrderDlqMessage msg) {
        if (dlqBuffer.size() >= MAX_BUFFER_SIZE) {
            dlqBuffer.pollFirst(); // Bỏ bớt tin nhắn cũ nhất nếu vượt quá giới hạn bộ đệm
        }
        dlqBuffer.addLast(msg);
    }

    public List<OrderDlqMessage> getDlqMessages() {
        return new ArrayList<>(dlqBuffer);
    }

    public int getDlqMessageCount() {
        return dlqBuffer.size();
    }

    public synchronized void clearDlqMessages() {
        dlqBuffer.clear();
    }

    private String getHeaderString(Map<String, Object> headers, String key) {
        if (headers == null || !headers.containsKey(key)) {
            return null;
        }
        Object val = headers.get(key);
        return val != null ? val.toString() : null;
    }

    private String extractExceptionClass(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return "UnknownException";
        }
        int newlineIdx = stackTrace.indexOf('\n');
        String firstLine = newlineIdx > 0 ? stackTrace.substring(0, newlineIdx).trim() : stackTrace.trim();
        int colonIdx = firstLine.indexOf(':');
        return colonIdx > 0 ? firstLine.substring(0, colonIdx).trim() : firstLine;
    }
}
