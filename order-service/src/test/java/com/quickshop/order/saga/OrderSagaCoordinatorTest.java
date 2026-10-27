package com.quickshop.order.saga;

import com.quickshop.common.event.OrderCreatedEvent;
import com.quickshop.common.event.RollbackEvent;
import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.PaymentMethod;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.saga.impl.OrderSagaCoordinatorImpl;
import com.quickshop.order.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaCoordinatorTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private VoucherService voucherService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OrderSagaCoordinatorImpl sagaCoordinator;

    private static final Long ORDER_ID = 888L;
    private static final String CORRELATION_ID = "corr-test-123";

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        sagaCoordinator = new OrderSagaCoordinatorImpl(
                rabbitTemplate,
                masterOrderRepository,
                voucherService,
                stringRedisTemplate
        );
    }

    @Test
    @DisplayName("1. Khởi chạy Saga: Gửi OrderCreatedEvent (lệnh giữ kho) sang RabbitMQ kèm correlationId và messageId")
    void testInitiateCheckoutSaga_Success() {
        // Given
        OrderItem item = OrderItem.builder()
                .variantId(201L)
                .productName("Chuột không dây")
                .variantName("Đen")
                .price(BigDecimal.valueOf(200000))
                .quantity(2)
                .build();

        SubOrder subOrder = SubOrder.builder()
                .id(1L)
                .shopId(10L)
                .items(List.of(item))
                .build();

        MasterOrder masterOrder = MasterOrder.builder()
                .id(ORDER_ID)
                .userId(1001L)
                .totalAmount(BigDecimal.valueOf(430000))
                .subOrders(List.of(subOrder))
                .build();

        // When
        sagaCoordinator.initiateCheckoutSaga(masterOrder);

        // Then
        // Kiểm tra lưu trạng thái vào Redis
        verify(valueOperations, times(1)).set(eq("saga:order:888:status"), eq("STOCK_RESERVE_PENDING"), any());
        verify(valueOperations, times(1)).set(eq("saga:order:888:correlation_id"), anyString(), any());

        // Kiểm tra gửi message sang RabbitMQ
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED),
                eventCaptor.capture(),
                any(MessagePostProcessor.class)
        );

        OrderCreatedEvent sentEvent = eventCaptor.getValue();
        assertEquals("888", sentEvent.getOrderId());
        assertEquals(1001L, sentEvent.getCustomerId());
        assertEquals(1, sentEvent.getItems().size());
        assertEquals(201L, sentEvent.getItems().get(0).getProductId());
        assertEquals(2, sentEvent.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("2. Xử lý giữ kho THÀNH CÔNG: Chuyển MasterOrder sang PENDING_PAYMENT và đánh dấu giữ kho trong Redis")
    void testHandleStockReservedSuccess() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(String.valueOf(ORDER_ID))
                .success(true)
                .reason("Stock reserved successfully")
                .build();

        MasterOrder order = MasterOrder.builder()
                .id(ORDER_ID)
                .status(MasterOrderStatus.DRAFT)
                .build();

        when(valueOperations.setIfAbsent(eq("saga:stock:reserved:processed:888"), eq("PROCESSED"), any()))
                .thenReturn(Boolean.TRUE); // Lần đầu xử lý
        when(masterOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // When
        sagaCoordinator.handleStockReservedSuccess(event, CORRELATION_ID);

        // Then
        assertEquals(MasterOrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(masterOrderRepository, times(1)).save(order);
        verify(valueOperations, times(1)).set(eq("saga:order:888:status"), eq("STOCK_RESERVED"), any());
        verify(valueOperations, times(1)).set(eq("saga:order:888:stock_reserved"), eq("true"), any());
    }

    @Test
    @DisplayName("3. Message giữ kho THÀNH CÔNG trùng lặp (Idempotent Consumer): Bỏ qua an toàn, không ghi DB lần 2")
    void testHandleStockReservedSuccess_DuplicateMessage_Ignored() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(String.valueOf(ORDER_ID))
                .success(true)
                .build();

        when(valueOperations.setIfAbsent(eq("saga:stock:reserved:processed:888"), eq("PROCESSED"), any()))
                .thenReturn(Boolean.FALSE); // Đã xử lý trước đó

        // When
        sagaCoordinator.handleStockReservedSuccess(event, CORRELATION_ID);

        // Then
        verify(masterOrderRepository, never()).findById(any());
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Xử lý giữ kho THẤT BẠI (Compensating Actions): Hủy đơn, hoàn lại voucher, KHÔNG hoàn kho và gửi RollbackEvent")
    void testHandleStockReservedFailed() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(String.valueOf(ORDER_ID))
                .success(false)
                .reason("Hết hàng trong kho")
                .build();

        SubOrder subOrder = SubOrder.builder()
                .id(1L)
                .shopId(10L)
                .shopVoucherCode("SHOP10VIP")
                .status(SubOrderStatus.CREATED)
                .build();

        MasterOrder order = MasterOrder.builder()
                .id(ORDER_ID)
                .status(MasterOrderStatus.DRAFT)
                .voucherCode("QUICKSHOP50")
                .subOrders(new ArrayList<>(List.of(subOrder)))
                .build();

        when(valueOperations.setIfAbsent(eq("saga:stock:failed:processed:888"), eq("PROCESSED"), any()))
                .thenReturn(Boolean.TRUE);
        when(masterOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        // When
        sagaCoordinator.handleStockReservedFailed(event, CORRELATION_ID);

        // Then
        // 1. Kiểm tra đơn hàng bị hủy
        assertEquals(MasterOrderStatus.CANCELLED, order.getStatus());
        assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
        verify(masterOrderRepository, times(1)).save(order);

        // 2. Kiểm tra hoàn lại Voucher đã dùng
        verify(voucherService, times(1)).releaseVouchers(
                eq("QUICKSHOP50"),
                argThat(list -> list.contains("SHOP10VIP") && list.size() == 1)
        );

        // 3. Kiểm tra gửi RollbackEvent sang RabbitMQ
        ArgumentCaptor<RollbackEvent> rollbackCaptor = ArgumentCaptor.forClass(RollbackEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_ROLLBACK),
                rollbackCaptor.capture()
        );
        assertEquals("888", rollbackCaptor.getValue().getOrderId());
        assertTrue(rollbackCaptor.getValue().getReason().contains("Hết hàng trong kho"));
    }

    @Test
    @DisplayName("5. Message giữ kho THẤT BẠI trùng lặp (Idempotent Consumer): Bỏ qua an toàn")
    void testHandleStockReservedFailed_DuplicateMessage_Ignored() {
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId(String.valueOf(ORDER_ID))
                .success(false)
                .build();

        when(valueOperations.setIfAbsent(eq("saga:stock:failed:processed:888"), eq("PROCESSED"), any()))
                .thenReturn(Boolean.FALSE);

        sagaCoordinator.handleStockReservedFailed(event, CORRELATION_ID);

        verify(masterOrderRepository, never()).findById(any());
        verify(voucherService, never()).releaseVouchers(any(), any());
    }

    @Test
    @DisplayName("6. Timeout 15 phút chưa thanh toán (cancelExpiredOrders): Hủy đơn, hoàn voucher và gửi lệnh Release Stock")
    void testCancelExpiredOrders_Success() {
        // Given
        SubOrder subOrder = SubOrder.builder()
                .id(1L)
                .shopVoucherCode("SHOP_TIMEOUT")
                .status(SubOrderStatus.CREATED)
                .build();

        MasterOrder expiredOrder = MasterOrder.builder()
                .id(777L)
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .voucherCode("PLATFORM_TIMEOUT")
                .createdAt(LocalDateTime.now().minusMinutes(20)) // 20 phút trước
                .subOrders(new ArrayList<>(List.of(subOrder)))
                .build();

        when(masterOrderRepository.findExpiredOrders(any(), any()))
                .thenReturn(List.of(expiredOrder));

        // When
        int cancelled = sagaCoordinator.cancelExpiredOrders();

        // Then
        assertEquals(1, cancelled);
        assertEquals(MasterOrderStatus.CANCELLED, expiredOrder.getStatus());
        assertEquals(SubOrderStatus.CANCELLED, subOrder.getStatus());
        verify(masterOrderRepository, times(1)).save(expiredOrder);

        // Hoàn voucher
        verify(voucherService, times(1)).releaseVouchers(
                eq("PLATFORM_TIMEOUT"),
                argThat(list -> list.contains("SHOP_TIMEOUT"))
        );

        // Gửi lệnh Release Stock sang Catalog Service (vì đã từng giữ kho)
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_CANCELLED),
                any(RollbackEvent.class)
        );
    }

    @Test
    @DisplayName("7. Đơn hàng không tồn tại trong CSDL -> Ném AmqpRejectAndDontRequeueException (chuyển thẳng DLQ, không retry)")
    void testHandleStockReserved_OrderNotFound_ThrowsNonRetryableException() {
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("99999")
                .success(true)
                .build();

        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(Boolean.TRUE);
        when(masterOrderRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                sagaCoordinator.handleStockReservedSuccess(event, CORRELATION_ID)
        );
    }
}
