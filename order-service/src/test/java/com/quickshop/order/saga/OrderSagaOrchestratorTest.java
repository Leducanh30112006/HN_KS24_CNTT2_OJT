package com.quickshop.order.saga;

import com.quickshop.common.event.*;
import com.quickshop.order.config.RabbitMQOrderConfig;
import com.quickshop.order.entity.*;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSagaOrchestrator - Bộ điều phối Saga Checkout, tiếp nhận kết quả và bồi hoàn giao dịch")
class OrderSagaOrchestratorTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private SubOrderRepository subOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private OrderSagaOrchestrator orchestrator;

    private MasterOrder codOrder;
    private MasterOrder vnpayOrder;
    private SubOrder subOrder1;
    private OrderItem orderItem1;

    @BeforeEach
    void setUp() {
        codOrder = MasterOrder.builder()
                .id(100L)
                .userId(1L)
                .totalAmount(BigDecimal.valueOf(500000))
                .status(MasterOrderStatus.DRAFT)
                .paymentMethod("COD")
                .shippingAddress("Hà Nội")
                .createdAt(LocalDateTime.now())
                .build();

        vnpayOrder = MasterOrder.builder()
                .id(200L)
                .userId(2L)
                .totalAmount(BigDecimal.valueOf(1200000))
                .status(MasterOrderStatus.DRAFT)
                .paymentMethod("VNPAY")
                .shippingAddress("Đà Nẵng")
                .createdAt(LocalDateTime.now())
                .build();

        subOrder1 = SubOrder.builder()
                .id(10L)
                .masterOrderId(100L)
                .shopId(5L)
                .status(SubOrderStatus.CREATED)
                .finalAmount(BigDecimal.valueOf(500000))
                .build();

        orderItem1 = OrderItem.builder()
                .id(1L)
                .subOrderId(10L)
                .productId(50L)
                .variantId(501L)
                .sku("SKU-501")
                .productName("Sản phẩm A")
                .price(BigDecimal.valueOf(250000))
                .quantity(2)
                .build();
    }

    @Test
    @DisplayName("Bắt đầu Saga Checkout: Phát OrderCreatedEvent với TTL 900 giây (15 phút)")
    void testStartCheckoutSaga_Success() {
        orchestrator.startCheckoutSaga(codOrder, List.of(orderItem1));

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_CREATED),
                captor.capture()
        );

        OrderCreatedEvent event = captor.getValue();
        assertThat(event.getMasterOrderId()).isEqualTo(100L);
        assertThat(event.getPaymentMethod()).isEqualTo("COD");
        assertThat(event.getTtlSeconds()).isEqualTo(900);
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getItems().get(0).getSku()).isEqualTo("SKU-501");
    }

    @Test
    @DisplayName("Nhận giữ kho thành công cho đơn COD: Chuyển PROCESSING, xác nhận SubOrders và phát OrderConfirmedEvent")
    void testHandleStockReserved_COD_Success() {
        when(masterOrderRepository.findById(100L)).thenReturn(Optional.of(codOrder));
        when(subOrderRepository.findByMasterOrderId(100L)).thenReturn(List.of(subOrder1));
        when(orderItemRepository.findBySubOrderIdIn(List.of(10L))).thenReturn(List.of(orderItem1));

        StockReservedEvent reservedEvent = StockReservedEvent.builder()
                .orderId("100")
                .masterOrderId(100L)
                .success(true)
                .build();

        orchestrator.handleStockReserved(reservedEvent);

        assertThat(codOrder.getStatus()).isEqualTo(MasterOrderStatus.PROCESSING);
        assertThat(subOrder1.getStatus()).isEqualTo(SubOrderStatus.CONFIRMED);
        verify(masterOrderRepository, times(1)).save(codOrder);
        verify(subOrderRepository, times(1)).save(subOrder1);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_CONFIRMED),
                any(OrderConfirmedEvent.class)
        );
    }

    @Test
    @DisplayName("Nhận giữ kho thành công cho đơn Online (VNPay): Chuyển PENDING_PAYMENT và phát PaymentRequestedEvent")
    void testHandleStockReserved_OnlinePayment_Success() {
        when(masterOrderRepository.findById(200L)).thenReturn(Optional.of(vnpayOrder));

        StockReservedEvent reservedEvent = StockReservedEvent.builder()
                .orderId("200")
                .masterOrderId(200L)
                .success(true)
                .build();

        orchestrator.handleStockReserved(reservedEvent);

        assertThat(vnpayOrder.getStatus()).isEqualTo(MasterOrderStatus.PENDING_PAYMENT);
        verify(masterOrderRepository, times(1)).save(vnpayOrder);

        ArgumentCaptor<PaymentRequestedEvent> captor = ArgumentCaptor.forClass(PaymentRequestedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_PAYMENT_REQUESTED),
                captor.capture()
        );

        PaymentRequestedEvent paymentEvent = captor.getValue();
        assertThat(paymentEvent.getMasterOrderId()).isEqualTo(200L);
        assertThat(paymentEvent.getTimeoutSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("Nhận giữ kho thất bại: Kích hoạt bồi hoàn, hủy đơn, hoàn voucher và phát StockReleaseEvent")
    void testHandleStockFailed_TriggersCompensation() {
        when(masterOrderRepository.findById(100L)).thenReturn(Optional.of(codOrder));
        when(subOrderRepository.findByMasterOrderId(100L)).thenReturn(List.of(subOrder1));
        when(orderItemRepository.findBySubOrderIdIn(List.of(10L))).thenReturn(List.of(orderItem1));

        StockReservedEvent failedEvent = StockReservedEvent.builder()
                .orderId("100")
                .masterOrderId(100L)
                .success(false)
                .reason("Kho chỉ còn 0 chiếc")
                .build();

        orchestrator.handleStockFailed(failedEvent);

        assertThat(codOrder.getStatus()).isEqualTo(MasterOrderStatus.CANCELLED);
        assertThat(subOrder1.getStatus()).isEqualTo(SubOrderStatus.CANCELLED);
        verify(voucherService, times(1)).revertUsage(100L);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_STOCK_RELEASE),
                any(StockReleaseEvent.class)
        );
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_COMPENSATION),
                any(OrderCompensationEvent.class)
        );
    }

    @Test
    @DisplayName("Nhận thanh toán thành công: Cập nhật PROCESSING, xác nhận SubOrders và phát OrderConfirmedEvent")
    void testHandlePaymentCompleted_Success() {
        when(masterOrderRepository.findById(200L)).thenReturn(Optional.of(vnpayOrder));
        when(subOrderRepository.findByMasterOrderId(200L)).thenReturn(List.of(subOrder1));
        when(orderItemRepository.findBySubOrderIdIn(List.of(10L))).thenReturn(List.of(orderItem1));

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                .orderId("200")
                .masterOrderId(200L)
                .transactionId("VNP123456")
                .amount(BigDecimal.valueOf(1200000))
                .build();

        orchestrator.handlePaymentCompleted(completedEvent);

        assertThat(vnpayOrder.getStatus()).isEqualTo(MasterOrderStatus.PROCESSING);
        assertThat(subOrder1.getStatus()).isEqualTo(SubOrderStatus.CONFIRMED);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQOrderConfig.EXCHANGE_ORDER),
                eq(RabbitMQOrderConfig.ROUTING_KEY_ORDER_CONFIRMED),
                any(OrderConfirmedEvent.class)
        );
    }

    @Test
    @DisplayName("Nhận thanh toán thất bại: Bồi hoàn và hủy đơn hàng")
    void testHandlePaymentFailed_TriggersCompensation() {
        when(masterOrderRepository.findById(200L)).thenReturn(Optional.of(vnpayOrder));
        when(subOrderRepository.findByMasterOrderId(200L)).thenReturn(List.of(subOrder1));
        when(orderItemRepository.findBySubOrderIdIn(List.of(10L))).thenReturn(List.of(orderItem1));

        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .orderId("200")
                .masterOrderId(200L)
                .reason("Số dư tài khoản không đủ")
                .build();

        orchestrator.handlePaymentFailed(failedEvent);

        assertThat(vnpayOrder.getStatus()).isEqualTo(MasterOrderStatus.CANCELLED);
        verify(voucherService, times(1)).revertUsage(200L);
    }

    @Test
    @DisplayName("Bồi hoàn đơn đã CANCELLED: Đảm bảo tính Idempotent (không thực hiện lại)")
    void testCancelOrder_AlreadyCancelled_Idempotent() {
        codOrder.setStatus(MasterOrderStatus.CANCELLED);
        when(masterOrderRepository.findById(100L)).thenReturn(Optional.of(codOrder));

        orchestrator.cancelOrderWithCompensation(100L, "Thử hủy lại đơn");

        verify(voucherService, never()).revertUsage(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
