package com.quickshop.catalog.consumer;

import com.quickshop.catalog.config.RabbitMQCatalogConfig;
import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.service.InventoryService;
import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.OrderItemEventDto;
import com.quickshop.common.event.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQCatalogConfig.QUEUE_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId: {} with {} items", event.getOrderId(),
                event.getItems() != null ? event.getItems().size() : 0);

        try {
            List<StockReserveItem> reserveItems = new ArrayList<>();
            if (event.getItems() != null) {
                for (OrderItemEventDto item : event.getItems()) {
                    reserveItems.add(StockReserveItem.builder()
                            .productId(item.getProductId())
                            .variantId(item.getVariantId())
                            .sku(item.getSku())
                            .quantity(item.getQuantity())
                            .build());
                }
            }

            // Real atomic inventory reservation in database
            inventoryService.reserveStock(event.getOrderId(), reserveItems);

            log.info("Stock successfully reserved for orderId: {}", event.getOrderId());
            StockReservedEvent successEvent = StockReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .success(true)
                    .reason("Giữ tồn kho thành công trong Catalog Service")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQCatalogConfig.EXCHANGE_CATALOG,
                    RabbitMQCatalogConfig.ROUTING_KEY_STOCK_RESERVED,
                    successEvent
            );

        } catch (Exception ex) {
            log.error("Failed to reserve stock for orderId: {}, error: {}", event.getOrderId(), ex.getMessage());
            StockReservedEvent failEvent = StockReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .success(false)
                    .reason(ex.getMessage() != null ? ex.getMessage() : "Hết hàng trong kho")
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQCatalogConfig.EXCHANGE_CATALOG,
                    RabbitMQCatalogConfig.ROUTING_KEY_STOCK_FAILED,
                    failEvent
            );
        }
    }
}
