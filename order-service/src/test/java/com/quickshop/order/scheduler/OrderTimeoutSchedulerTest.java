package com.quickshop.order.scheduler;

import com.quickshop.common.event.RollbackEvent;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.saga.impl.OrderSagaCoordinatorImpl;
import com.quickshop.order.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTimeoutScheduler Tests - Quét hủy đơn quá hạn 15 phút & Bảo vệ đơn đã thanh toán")
class OrderTimeoutSchedulerTest {

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private VoucherService voucherService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OrderSagaCoordinatorImpl sagaCoordinator;
    private OrderTimeoutScheduler timeoutScheduler;

    @BeforeEach
    void setUp() {
        sagaCoordinator = new OrderSagaCoordinatorImpl(
                rabbitTemplate,
                masterOrderRepository,
                voucherService,
                stringRedisTemplate
        );
        timeoutScheduler = new OrderTimeoutScheduler(sagaCoordinator);
    }

    @Test
    @DisplayName("Tác vụ 1: Đơn quá hạn 15 phút ở trạng thái PENDING_PAYMENT => Bị hủy, nhả voucher và gửi Release Stock qua RabbitMQ")
    void testScanAndCancelExpiredOrders_ExpiredOrder_CancelledSuccessfully() {
        MasterOrder expiredOrder = MasterOrder.builder()
                .id(1001L)
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .subOrders(new ArrayList<>())
                .voucherCode("DISCOUNT10")
                .build();

        SubOrder subOrder = SubOrder.builder()
                .id(2001L)
                .masterOrder(expiredOrder)
                .status(SubOrderStatus.CREATED)
                .shopVoucherCode("SHOP5")
                .build();
        expiredOrder.getSubOrders().add(subOrder);

        when(masterOrderRepository.findExpiredOrders(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredOrder));
        when(masterOrderRepository.findById(1001L)).thenReturn(Optional.of(expiredOrder));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        timeoutScheduler.scanAndCancelExpiredOrders();

        // 1. Kiểm tra trạng thái đơn hàng đã chuyển sang CANCELLED
        assertEquals(MasterOrderStatus.CANCELLED, expiredOrder.getStatus());
        assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
        verify(masterOrderRepository).save(expiredOrder);

        // 2. Kiểm tra hoàn voucher sàn và shop
        verify(voucherService).releaseVouchers(eq("DISCOUNT10"), eq(List.of("SHOP5")));

        // 3. Kiểm tra gửi lệnh bù trừ kho Release Stock qua RabbitMQ
        verify(rabbitTemplate).convertAndSend(
                eq("order.exchange"),
                eq("order.cancelled"),
                any(RollbackEvent.class)
        );
    }

    @Test
    @DisplayName("Tác vụ 1: Đơn tạo quá 15 phút nhưng trạng thái mới nhất là PROCESSING (đã thanh toán) => KHÔNG BỊ HỦY")
    void testScanAndCancelExpiredOrders_OrderAlreadyPaid_ProtectedFromCancellation() {
        MasterOrder staleOrder = MasterOrder.builder()
                .id(1002L)
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now().minusMinutes(16))
                .build();

        // Cơ sở dữ liệu ghi nhận đơn đã được thanh toán thành công (PROCESSING)
        MasterOrder freshPaidOrder = MasterOrder.builder()
                .id(1002L)
                .status(MasterOrderStatus.PROCESSING)
                .createdAt(LocalDateTime.now().minusMinutes(16))
                .build();

        when(masterOrderRepository.findExpiredOrders(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(staleOrder));
        when(masterOrderRepository.findById(1002L)).thenReturn(Optional.of(freshPaidOrder));

        timeoutScheduler.scanAndCancelExpiredOrders();

        // Kiểm tra đơn hàng không bị hủy và không lưu trạng thái CANCELLED
        assertEquals(MasterOrderStatus.PROCESSING, freshPaidOrder.getStatus());
        verify(masterOrderRepository, never()).save(freshPaidOrder);

        // Tuyệt đối không gửi Release Stock cho đơn đã thanh toán
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(voucherService, never()).releaseVouchers(any(), any());
    }

    @Test
    @DisplayName("Tác vụ 1: Không có đơn nào quá hạn => Không thực hiện thao tác hủy nào")
    void testScanAndCancelExpiredOrders_NoExpiredOrders_NoAction() {
        when(masterOrderRepository.findExpiredOrders(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        timeoutScheduler.scanAndCancelExpiredOrders();

        verify(masterOrderRepository, never()).findById(anyLong());
        verify(masterOrderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
