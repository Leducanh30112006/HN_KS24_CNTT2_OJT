package com.quickshop.catalog.consumer;

import com.quickshop.catalog.config.RabbitMQCatalogConfig;
import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.service.InventoryService;
import com.quickshop.common.event.OrderConfirmedEvent;
import com.quickshop.common.event.OrderItemEventDto;
import com.quickshop.common.event.StockReleaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockCompensationConsumer {

    private final InventoryService inventoryService;

    @RabbitListener(queues = RabbitMQCatalogConfig.QUEUE_STOCK_RELEASE)
    public void handleStockRelease(StockReleaseEvent event) {
        String orderId = event.getOrderId() != null ? event.getOrderId() : String.valueOf(event.getMasterOrderId());
        log.info("Received StockReleaseEvent for order: {}, reason: {}", orderId, event.getReason());

        try {
            List<StockReserveItem> items = toStockReserveItems(event.getItems());
            inventoryService.releaseStock(orderId, items);
            log.info("Successfully released stock and removed Redis reservation TTL for order: {}", orderId);
        } catch (Exception ex) {
            log.error("Failed to release stock for order: {}, error: {}", orderId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @RabbitListener(queues = RabbitMQCatalogConfig.QUEUE_ORDER_CONFIRMED)
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        String orderId = event.getOrderId() != null ? event.getOrderId() : String.valueOf(event.getMasterOrderId());
        log.info("Received OrderConfirmedEvent for order: {}", orderId);

        try {
            List<StockReserveItem> items = toStockReserveItems(event.getItems());
            inventoryService.commitStock(orderId, items);
            log.info("Successfully committed stock and removed Redis reservation TTL for order: {}", orderId);
        } catch (Exception ex) {
            log.error("Failed to commit stock for order: {}, error: {}", orderId, ex.getMessage(), ex);
            throw ex;
        }
    }

    private List<StockReserveItem> toStockReserveItems(List<OrderItemEventDto> eventItems) {
        List<StockReserveItem> items = new ArrayList<>();
        if (eventItems != null) {
            for (OrderItemEventDto item : eventItems) {
                items.add(StockReserveItem.builder()
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .build());
            }
        }
        return items;
    }
}
