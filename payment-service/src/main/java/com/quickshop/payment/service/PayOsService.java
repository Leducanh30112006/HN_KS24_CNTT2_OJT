package com.quickshop.payment.service;

import com.quickshop.payment.dto.CreatePayOsPaymentRequest;
import com.quickshop.payment.dto.CreatePayOsPaymentResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service chuyên biệt tích hợp thư viện PayOS:
 * - Tạo liên kết thanh toán / chuyển tiền qua mã QR MB Bank chỉ cần nhận orderId
 * - Tự động tra cứu thông tin đơn hàng và đóng gói request/response
 * - Kiểm tra trạng thái xem đơn hàng đã chuyển tiền chưa (Active Polling)
 * - Tiếp nhận và xác thực Webhook biến động số dư từ PayOS
 * - Xác nhận tự động Webhook URL với PayOS server
 * - Hủy liên kết thanh toán khi đơn hàng quá hạn hoặc bị hủy
 */
public interface PayOsService {

    /**
     * Tạo yêu cầu thanh toán chuyển khoản PayOS chỉ cần nhận orderId.
     * Service sẽ tự động tra cứu số tiền đơn hàng từ database/Order Service,
     * tự khởi tạo CreatePayOsPaymentRequest và đóng gói CreatePayOsPaymentResponse.
     *
     * @param orderId Mã đơn hàng (ví dụ: "1001", "QS-1001")
     * @return CreatePayOsPaymentResponse chứa checkoutUrl, qrCode, stk, tên chủ tài khoản
     */
    CreatePayOsPaymentResponse createPayment(String orderId);

    /**
     * Tạo yêu cầu thanh toán PayOS với orderId và số tiền tùy chỉnh
     *
     * @param orderId Mã đơn hàng
     * @param customAmount Số tiền cần thanh toán
     * @return CreatePayOsPaymentResponse
     */
    CreatePayOsPaymentResponse createPayment(String orderId, BigDecimal customAmount);

    /**
     * Tạo yêu cầu thanh toán chuyển khoản PayOS đầy đủ thông qua Request DTO
     *
     * @param request Thông tin đơn hàng, số tiền, URL chuyển hướng
     * @return CreatePayOsPaymentResponse
     */
    CreatePayOsPaymentResponse createPaymentLink(CreatePayOsPaymentRequest request);

    /**
     * Kiểm tra trạng thái xem đã chuyển tiền chưa:
     * Chủ động gọi PayOS API (paymentRequests().get) và tra cứu database
     *
     * @param orderId Mã đơn hàng cần kiểm tra
     * @return Map chứa { paid: boolean, status: String, orderId: String, amount: BigDecimal, ... }
     */
    Map<String, Object> checkPaymentStatus(String orderId);

    /**
     * Xử lý Webhook thông báo chuyển khoản thành công từ PayOS:
     * - Xác thực chữ ký điện tử bằng PayOS SDK
     * - Đảm bảo tính Idempotent (chống xử lý lặp lại)
     * - Cập nhật trạng thái SUCCESS
     * - Ghi nhận sổ cái kép (Ledger)
     * - Bắn sự kiện PaymentCompletedEvent tới Order Service
     *
     * @param webhookData Payload JSON do PayOS gửi sang
     * @return Map phản hồi chuẩn PayOS { error: 0, message: "..." }
     */
    Map<String, Object> processWebhook(Map<String, Object> webhookData);

    /**
     * Xác nhận và đăng ký Webhook URL với PayOS API
     *
     * @param webhookUrl URL công khai của Webhook (ví dụ: https://.../api/payments/payos/webhook)
     * @return Kết quả xác nhận từ PayOS
     */
    String confirmWebhook(String webhookUrl);

    /**
     * Hủy liên kết thanh toán PayOS của đơn hàng
     *
     * @param orderId Mã đơn hàng
     * @param reason Lý do hủy
     * @return Map thông tin kết quả hủy
     */
    Map<String, Object> cancelPaymentLink(String orderId, String reason);
}
