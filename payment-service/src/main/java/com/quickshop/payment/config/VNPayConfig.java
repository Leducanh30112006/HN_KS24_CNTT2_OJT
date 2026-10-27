package com.quickshop.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {
    /**
     * Mã website của merchant trên hệ thống của VNPay (vnp_TmnCode)
     */
    private String tmnCode = "QUICKSHOP";

    /**
     * Chuỗi bí mật dùng để tạo và xác thực chữ ký HMAC-SHA512 (vnp_HashSecret)
     */
    private String hashSecret = "QUICKSHOPSECRETKEYVNPAY2026";

    /**
     * URL chuyển hướng người dùng sang cổng thanh toán VNPay Sandbox
     */
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /**
     * URL nhận kết quả trả về từ VNPay khi người dùng hoàn tất giao dịch
     */
    private String returnUrl = "http://localhost:5173/payment/vnpay-return";
}
