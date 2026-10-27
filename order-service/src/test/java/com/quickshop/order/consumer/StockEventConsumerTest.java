package com.quickshop.order.consumer;

import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.saga.OrderSagaCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockEventConsumerTest {

    @Mock
    private OrderSagaCoordinator orderSagaCoordinator;

    @InjectMocks
    private StockEventConsumer stockEventConsumer;

    @Test
    @DisplayName("Consumer tiếp nhận StockReservedEvent và trích xuất đúng CorrelationId chuyển tiếp tới Coordinator")
    void testOnStockReserved_Success() {
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("100")
                .success(true)
                .build();

        MessageProperties properties = new MessageProperties();
        properties.setCorrelationId("test-corr-id-123");
        Message message = new Message(new byte[0], properties);

        stockEventConsumer.onStockReserved(event, message);

        verify(orderSagaCoordinator, times(1)).handleStockReservedSuccess(eq(event), eq("test-corr-id-123"));
    }

    @Test
    @DisplayName("Consumer tiếp nhận StockFailedEvent và chuyển tiếp tới Coordinator")
    void testOnStockFailed_Success() {
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("100")
                .success(false)
                .reason("Out of stock")
                .build();

        MessageProperties properties = new MessageProperties();
        properties.setHeader("correlationId", "header-corr-id");
        Message message = new Message(new byte[0], properties);

        stockEventConsumer.onStockFailed(event, message);

        verify(orderSagaCoordinator, times(1)).handleStockReservedFailed(eq(event), eq("header-corr-id"));
    }

    @Test
    @DisplayName("Consumer ném lại AmqpRejectAndDontRequeueException để chuyển thẳng vào DLQ mà không retry")
    void testOnStockReserved_NonRetryableException_Propagated() {
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("999")
                .success(true)
                .build();

        MessageProperties properties = new MessageProperties();
        Message message = new Message(new byte[0], properties);

        doThrow(new AmqpRejectAndDontRequeueException("Non-retryable error"))
                .when(orderSagaCoordinator).handleStockReservedSuccess(any(), any());

        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                stockEventConsumer.onStockReserved(event, message)
        );
    }
}
