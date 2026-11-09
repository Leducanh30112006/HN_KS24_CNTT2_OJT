package com.quickshop.catalog.consumer;

import com.quickshop.catalog.service.InventoryService;
import com.quickshop.common.event.OrderConfirmedEvent;
import com.quickshop.common.event.OrderItemEventDto;
import com.quickshop.common.event.StockReleaseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockCompensationConsumer - Tiếp nhận sự kiện bồi hoàn kho và xác nhận thanh toán")
class StockCompensationConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private StockCompensationConsumer consumer;

    @Test
    @DisplayName("Nhận StockReleaseEvent: Giải phóng tồn kho cho đơn hàng")
    void testHandleStockRelease() {
        StockReleaseEvent event = StockReleaseEvent.builder()
                .orderId("100")
                .masterOrderId(100L)
                .reason("Khách hủy đơn")
                .items(List.of(
                        OrderItemEventDto.builder().variantId(1L).quantity(2).build()
                ))
                .build();

        consumer.handleStockRelease(event);

        verify(inventoryService, times(1)).releaseStock(eq("100"), anyList());
    }

    @Test
    @DisplayName("Nhận OrderConfirmedEvent: Commit tồn kho chính thức cho đơn hàng")
    void testHandleOrderConfirmed() {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId("100")
                .masterOrderId(100L)
                .items(List.of(
                        OrderItemEventDto.builder().variantId(1L).quantity(2).build()
                ))
                .build();

        consumer.handleOrderConfirmed(event);

        verify(inventoryService, times(1)).commitStock(eq("100"), anyList());
    }
}
