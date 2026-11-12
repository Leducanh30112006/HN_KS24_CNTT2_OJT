package com.quickshop.payment.consumer;

import com.quickshop.common.event.PaymentRequestedEvent;
import com.quickshop.payment.config.RabbitMQPaymentConfig;
import com.quickshop.payment.dto.CreateVNPayPaymentRequest;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.service.PayOsService;
import com.quickshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderEventConsumer {

    private final PaymentService paymentService;
    private final PayOsService payOsService;

    @RabbitListener(queues = RabbitMQPaymentConfig.QUEUE_PAYMENT_REQUESTED)
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        if (event == null) {
            log.warn("Nhận thông điệp PaymentRequestedEvent null, bỏ qua");
            return;
        }

        log.info("Tiếp nhận PaymentRequestedEvent từ Order Saga: orderId={}, amount={}, paymentMethod={}, userId={}",
                event.getOrderId(), event.getAmount(), event.getPaymentMethod(), event.getUserId());

        String method = event.getPaymentMethod() != null ? event.getPaymentMethod().trim().toUpperCase() : "";

        if ("VNPAY".equalsIgnoreCase(method)) {
            try {
                CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                        .orderId(event.getOrderId())
                        .amount(event.getAmount())
                        .userId(event.getUserId())
                        .orderInfo("Thanh toan don hang #" + event.getOrderId())
                        .build();

                VNPayPaymentResponse response = paymentService.createVNPayPayment(request, "127.0.0.1");
                log.info("Đã tự động khởi tạo giao dịch VNPay cho đơn hàng #{}: txnRef={}, url={}",
                        event.getOrderId(), response.getTxnRef(), response.getPaymentUrl());
            } catch (Exception e) {
                log.error("Lỗi khi xử lý tạo giao dịch VNPay từ sự kiện Order Saga cho đơn hàng #{}: {}",
                        event.getOrderId(), e.getMessage(), e);
                throw e; // Để RabbitMQ retry hoặc đưa vào DLQ nếu cấu hình
            }
        } else if ("PAYOS".equalsIgnoreCase(method) || "PAYOS_MBBANK".equalsIgnoreCase(method) || "MBBANK".equalsIgnoreCase(method)) {
            try {
                payOsService.createPayment(event.getOrderId(), event.getAmount());
                log.info("Đã tự động khởi tạo yêu cầu thanh toán PayOS MB Bank cho đơn hàng #{}", event.getOrderId());
            } catch (Exception e) {
                log.error("Lỗi khi xử lý tạo yêu cầu thanh toán PayOS từ sự kiện Order Saga cho đơn hàng #{}: {}",
                        event.getOrderId(), e.getMessage(), e);
                throw e;
            }
        } else {
            log.info("Bỏ qua sự kiện yêu cầu thanh toán do không phải cổng thanh toán trực tuyến (COD / khác): {}", method);
        }
    }
}
