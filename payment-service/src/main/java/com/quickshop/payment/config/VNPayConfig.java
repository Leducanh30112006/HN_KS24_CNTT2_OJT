package com.quickshop.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {

    private String tmnCode = "QUICKSHOP";
    private String hashSecret = "QUICKSHOPSECRETKEYVNPAY2026";
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:5173/payment/vnpay-return";
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String currCode = "VND";
    private String locale = "vn";
    private int expireMinutes = 15;
}
