package com.quickshop.order.saga;

import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.MasterOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExpirationScheduler - Quét và xử lý tự động timeout giữ kho 15 phút")
class OrderExpirationSchedulerTest {

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private OrderExpirationScheduler scheduler;

    private MasterOrder expiredOrder1;
    private MasterOrder expiredOrder2;

    @BeforeEach
    void setUp() {
        expiredOrder1 = MasterOrder.builder()
                .id(101L)
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        expiredOrder2 = MasterOrder.builder()
                .id(102L)
                .status(MasterOrderStatus.DRAFT)
                .createdAt(LocalDateTime.now().minusMinutes(16))
                .build();
    }

    @Test
    @DisplayName("Tìm thấy 2 đơn quá hạn 15 phút: Kích hoạt hủy và bồi hoàn cả 2 đơn")
    void testCheckAndCancelExpiredOrders_WhenOrdersFound() {
        when(masterOrderRepository.findByStatusInAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of(expiredOrder1, expiredOrder2));

        int processed = scheduler.checkAndCancelExpiredOrders();

        assertThat(processed).isEqualTo(2);
        verify(sagaOrchestrator, times(1)).cancelOrderWithCompensation(eq(101L), contains("15 phút"));
        verify(sagaOrchestrator, times(1)).cancelOrderWithCompensation(eq(102L), contains("15 phút"));
    }

    @Test
    @DisplayName("Không có đơn nào quá hạn: Không gọi bồi hoàn")
    void testCheckAndCancelExpiredOrders_WhenNoOrdersFound() {
        when(masterOrderRepository.findByStatusInAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of());

        int processed = scheduler.checkAndCancelExpiredOrders();

        assertThat(processed).isEqualTo(0);
        verify(sagaOrchestrator, never()).cancelOrderWithCompensation(any(), any());
    }
}
