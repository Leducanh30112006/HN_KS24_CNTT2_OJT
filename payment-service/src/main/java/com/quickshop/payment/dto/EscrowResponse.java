package com.quickshop.payment.dto;

import com.quickshop.payment.entity.enums.EscrowStatus;
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
public class EscrowResponse {
    private Long id;
    private Long subOrderId;
    private Long masterOrderId;
    private Long shopId;
    private BigDecimal amount;
    private BigDecimal commissionFee;
    private BigDecimal netSellerAmount;
    private EscrowStatus status;
    @com.fasterxml.jackson.annotation.JsonProperty("isDisputed")
    private boolean isDisputed;
    private String disputeReason;
    private LocalDateTime disputedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime releasedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
