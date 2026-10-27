package com.quickshop.order.consumer;

import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.saga.OrderSagaCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final OrderSagaCoordinator orderSagaCoordinator;

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_STOCK_RESERVED)
    public void onStockReserved(StockReservedEvent event, Message amqpMessage) {
        String correlationId = extractCorrelationId(amqpMessage);
        log.info("[Saga Consumer][CorrelationID: {}] Tiếp nhận StockReservedEvent cho orderId: {}",
                correlationId, event.getOrderId());

        try {
            orderSagaCoordinator.handleStockReservedSuccess(event, correlationId);
        } catch (AmqpRejectAndDontRequeueException ex) {
            log.error("[Saga Consumer][CorrelationID: {}] Lỗi nghiệp vụ vĩnh viễn (không retry, chuyển thẳng DLQ): {}",
                    correlationId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[Saga Consumer][CorrelationID: {}] Lỗi tạm thời khi xử lý giữ kho thành công: {}",
                    correlationId, ex.getMessage(), ex);
            throw ex; // Spring AMQP RetryTemplate sẽ retry tối đa 3 lần rồi chuyển sang order.dlq
        }
    }

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_STOCK_FAILED)
    public void onStockFailed(StockReservedEvent event, Message amqpMessage) {
        String correlationId = extractCorrelationId(amqpMessage);
        log.warn("[Saga Consumer][CorrelationID: {}] Tiếp nhận StockFailedEvent cho orderId: {}, Lý do: {}",
                correlationId, event.getOrderId(), event.getReason());

        try {
            orderSagaCoordinator.handleStockReservedFailed(event, correlationId);
        } catch (AmqpRejectAndDontRequeueException ex) {
            log.error("[Saga Consumer][CorrelationID: {}] Lỗi nghiệp vụ vĩnh viễn (không retry, chuyển thẳng DLQ): {}",
                    correlationId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[Saga Consumer][CorrelationID: {}] Lỗi tạm thời khi xử lý giữ kho thất bại: {}",
                    correlationId, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String extractCorrelationId(Message amqpMessage) {
        if (amqpMessage != null && amqpMessage.getMessageProperties() != null) {
            String correlationId = amqpMessage.getMessageProperties().getCorrelationId();
            if (correlationId != null && !correlationId.isBlank()) {
                return correlationId;
            }
            Object headerId = amqpMessage.getMessageProperties().getHeader("correlationId");
            if (headerId != null) {
                return String.valueOf(headerId);
            }
        }
        return UUID.randomUUID().toString();
    }
}
