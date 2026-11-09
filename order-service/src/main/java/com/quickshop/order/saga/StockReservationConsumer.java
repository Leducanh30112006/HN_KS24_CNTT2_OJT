package com.quickshop.order.saga;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.event.PaymentFailedEvent;
import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer tiếp nhận phản hồi từ các service thành viên trong chu trình Saga:
 * - Tiếp nhận kết quả giữ tồn kho (StockReservedEvent) từ Catalog Service
 * - Tiếp nhận kết quả thanh toán (PaymentCompletedEvent / PaymentFailedEvent) từ Payment Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservationConsumer {

    private final OrderSagaOrchestrator sagaOrchestrator;

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_STOCK_RESERVED)
    public void onStockReserved(StockReservedEvent event) {
        log.info("StockReservationConsumer: Nhận thông điệp giữ kho thành công cho order: {}", event.getOrderId());
        try {
            sagaOrchestrator.handleStockReserved(event);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý StockReservedEvent cho order {}: {}", event.getOrderId(), ex.getMessage(), ex);
            throw ex; // Kích hoạt retry qua RabbitMQ RetryOperationsInterceptor
        }
    }

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_STOCK_FAILED)
    public void onStockFailed(StockReservedEvent event) {
        log.warn("StockReservationConsumer: Nhận thông điệp giữ kho thất bại cho order: {}, lý do: {}",
                event.getOrderId(), event.getReason());
        try {
            sagaOrchestrator.handleStockFailed(event);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý StockFailedEvent cho order {}: {}", event.getOrderId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_PAYMENT_COMPLETED)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("StockReservationConsumer: Nhận thông điệp thanh toán thành công cho order: {}", event.getOrderId());
        try {
            sagaOrchestrator.handlePaymentCompleted(event);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý PaymentCompletedEvent cho order {}: {}", event.getOrderId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @RabbitListener(queues = RabbitMQOrderConfig.QUEUE_PAYMENT_FAILED)
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("StockReservationConsumer: Nhận thông điệp thanh toán thất bại cho order: {}", event.getOrderId());
        try {
            sagaOrchestrator.handlePaymentFailed(event);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý PaymentFailedEvent cho order {}: {}", event.getOrderId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
