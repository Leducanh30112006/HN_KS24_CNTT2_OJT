package com.quickshop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePayOsPaymentResponse {

    private Long orderCode;
    private String orderId;
    private BigDecimal amount;
    private String checkoutUrl;
    private String qrCode;
    private String accountNumber;
    private String accountName;
    private String bin;
    private String paymentLinkId;
    private String status;
}

