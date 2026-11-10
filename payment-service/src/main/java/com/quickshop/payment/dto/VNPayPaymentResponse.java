package com.quickshop.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quickshop.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentResponse {

    private String txnRef;
    private String orderId;
    private BigDecimal amount;
    private String paymentUrl;
    private PaymentStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireAt;

    private String message;
}
