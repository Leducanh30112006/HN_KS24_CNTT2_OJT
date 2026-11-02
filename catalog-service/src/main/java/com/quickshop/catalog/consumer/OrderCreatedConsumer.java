package com.quickshop.catalog.consumer;

import com.quickshop.catalog.config.RabbitMQCatalogConfig;
import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQCatalogConfig.QUEUE_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId: {} with {} items", event.getOrderId(), event.getItems().size());

        try {
            // Inventory check & reservation logic
            boolean stockAvailable = true; // In real DB check, verify each product's stock_quantity >= item.quantity

            if (stockAvailable) {
                log.info("Stock successfully reserved for orderId: {}", event.getOrderId());
                StockReservedEvent successEvent = StockReservedEvent.builder()
                        .orderId(event.getOrderId())
                        .success(true)
                        .reason("Stock reserved successfully")
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQCatalogConfig.EXCHANGE_CATALOG,
                        RabbitMQCatalogConfig.ROUTING_KEY_STOCK_RESERVED,
                        successEvent
                );
            } else {
                throw new IllegalStateException("Hết hàng trong kho");
            }
        } catch (Exception ex) {
            log.error("Failed to reserve stock for orderId: {}, error: {}", event.getOrderId(), ex.getMessage());
            StockReservedEvent failEvent = StockReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .success(false)
                    .reason(ex.getMessage())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQCatalogConfig.EXCHANGE_CATALOG,
                    RabbitMQCatalogConfig.ROUTING_KEY_STOCK_FAILED,
                    failEvent
            );
        }
    }
}
