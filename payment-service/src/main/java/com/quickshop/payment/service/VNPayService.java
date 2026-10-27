package com.quickshop.payment.service;

import com.quickshop.payment.dto.VNPayCreatePaymentRequest;
import com.quickshop.payment.dto.VNPayIpnResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.dto.VNPayReturnResponse;
import com.quickshop.payment.entity.PaymentTransaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VNPayService {

    /**
     * Tạo URL chuyển hướng thanh toán VNPay Sandbox
     */
    VNPayPaymentResponse createPaymentUrl(VNPayCreatePaymentRequest request, HttpServletRequest servletRequest);

    /**
     * Tiếp nhận và xử lý Server-to-Server IPN Webhook từ VNPay
     */
    VNPayIpnResponse processIpn(Map<String, String> params);

    /**
     * Xử lý kết quả trả về từ trình duyệt (Return URL)
     * Tuyệt đối không dùng Return URL làm bằng chứng thanh toán thành công
     */
    VNPayReturnResponse processReturn(Map<String, String> params);

    /**
     * Lấy thông tin giao dịch theo txnRef
     */
    PaymentTransaction getTransactionByTxnRef(String txnRef);

    /**
     * Lấy thông tin giao dịch mới nhất theo orderId
     */
    PaymentTransaction getLatestTransactionByOrderId(Long orderId);
}
