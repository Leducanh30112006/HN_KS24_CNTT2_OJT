package com.quickshop.order.saga;

import com.quickshop.common.event.OrderDlqMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderDlqListener - Lắng nghe Dead-Letter Queue order.dlq và ghi nhận lỗi sau 3 lần retry")
class OrderDlqListenerTest {

    private OrderDlqListener dlqListener;

    @BeforeEach
    void setUp() {
        dlqListener = new OrderDlqListener();
        dlqListener.clearDlqMessages();
    }

    @Test
    @DisplayName("Nhận message từ order.dlq: Trích xuất headers và lưu vào bộ nhớ đệm giám sát")
    void testOnDeadLetterMessage_ExtractsHeadersAndRecords() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-original-exchange", "order.exchange");
        properties.setHeader("x-original-routingKey", "order.created");
        properties.setHeader("x-exception-message", "Connection timeout to inventory database");
        properties.setHeader("x-exception-stacktrace", "org.springframework.dao.QueryTimeoutException: Timeout\n\tat com.quickshop...");

        String payload = "{\"orderId\":\"12345\",\"totalAmount\":500000}";
        Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), properties);

        dlqListener.onDeadLetterMessage(message);

        List<OrderDlqMessage> dlqMessages = dlqListener.getDlqMessages();
        assertThat(dlqMessages).hasSize(1);

        OrderDlqMessage recorded = dlqMessages.get(0);
        assertThat(recorded.getOriginalExchange()).isEqualTo("order.exchange");
        assertThat(recorded.getOriginalRoutingKey()).isEqualTo("order.created");
        assertThat(recorded.getErrorMessage()).isEqualTo("Connection timeout to inventory database");
        assertThat(recorded.getExceptionClass()).isEqualTo("org.springframework.dao.QueryTimeoutException");
        assertThat(recorded.getRetryCount()).isEqualTo(3);
        assertThat(recorded.getPayload()).contains("12345");
    }

    @Test
    @DisplayName("Xóa bộ đệm DLQ: Làm sạch toàn bộ thông điệp")
    void testClearDlqMessages() {
        OrderDlqMessage msg = OrderDlqMessage.builder()
                .originalExchange("order.exchange")
                .originalRoutingKey("order.created")
                .errorMessage("Test Error")
                .build();

        dlqListener.recordMessage(msg);
        assertThat(dlqListener.getDlqMessageCount()).isEqualTo(1);

        dlqListener.clearDlqMessages();
        assertThat(dlqListener.getDlqMessageCount()).isEqualTo(0);
    }
}
