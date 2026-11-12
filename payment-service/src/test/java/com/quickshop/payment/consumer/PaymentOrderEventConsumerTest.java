package com.quickshop.payment.consumer;

import com.quickshop.common.event.PaymentRequestedEvent;
import com.quickshop.payment.dto.CreateVNPayPaymentRequest;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private com.quickshop.payment.service.PayOsService payOsService;

    @InjectMocks
    private PaymentOrderEventConsumer consumer;

    @Test
    @DisplayName("handlePaymentRequested: Nhận sự kiện với paymentMethod=VNPAY -> Gọi paymentService.createVNPayPayment")
    void testHandlePaymentRequestedVNPAY() {
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .orderId("ORD_888")
                .masterOrderId(888L)
                .userId(12L)
                .amount(BigDecimal.valueOf(150000))
                .paymentMethod("VNPAY")
                .timeoutSeconds(900)
                .build();

        VNPayPaymentResponse mockResponse = VNPayPaymentResponse.builder()
                .orderId("ORD_888")
                .txnRef("ORD_888_12345")
                .amount(BigDecimal.valueOf(150000))
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.createVNPayPayment(any(CreateVNPayPaymentRequest.class), eq("127.0.0.1")))
                .thenReturn(mockResponse);

        consumer.handlePaymentRequested(event);

        ArgumentCaptor<CreateVNPayPaymentRequest> captor = ArgumentCaptor.forClass(CreateVNPayPaymentRequest.class);
        verify(paymentService, times(1)).createVNPayPayment(captor.capture(), eq("127.0.0.1"));

        CreateVNPayPaymentRequest captured = captor.getValue();
        assertThat(captured.getOrderId()).isEqualTo("ORD_888");
        assertThat(captured.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150000));
        assertThat(captured.getUserId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("handlePaymentRequested: Nhận sự kiện với paymentMethod=PAYOS -> Gọi payOsService.createPayment")
    void testHandlePaymentRequestedPAYOS() {
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .orderId("ORD_777")
                .masterOrderId(777L)
                .userId(18L)
                .amount(BigDecimal.valueOf(250000))
                .paymentMethod("PAYOS")
                .build();

        consumer.handlePaymentRequested(event);

        verify(payOsService, times(1)).createPayment("ORD_777", BigDecimal.valueOf(250000));
        verify(paymentService, never()).createVNPayPayment(any(), any());
    }

    @Test
    @DisplayName("handlePaymentRequested: Nhận sự kiện với phương thức COD -> Không gọi paymentService.createVNPayPayment")
    void testHandlePaymentRequestedCOD() {
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .orderId("ORD_999")
                .masterOrderId(999L)
                .userId(15L)
                .amount(BigDecimal.valueOf(300000))
                .paymentMethod("COD")
                .build();

        consumer.handlePaymentRequested(event);

        verify(paymentService, never()).createVNPayPayment(any(), any());
    }

    @Test
    @DisplayName("handlePaymentRequested: Nhận event null -> Bỏ qua an toàn không gây exception")
    void testHandlePaymentRequestedNullEvent() {
        consumer.handlePaymentRequested(null);
        verify(paymentService, never()).createVNPayPayment(any(), any());
    }
}
