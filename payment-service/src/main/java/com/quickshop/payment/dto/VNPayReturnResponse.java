package com.quickshop.payment.dto;

import com.quickshop.payment.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayReturnResponse {
    private Long orderId;
    private String txnRef;
    private BigDecimal amount;
    private String vnpayTransactionNo;
    private String bankCode;
    private String responseCode;
    private PaymentStatus status;
    private boolean validSignature;
    private boolean success;
    private String message;
}
