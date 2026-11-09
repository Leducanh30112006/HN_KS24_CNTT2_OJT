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
public class HoldEscrowRequest {
    private Long subOrderId;
    private Long shopId;
    private BigDecimal amount;
    private BigDecimal commissionFee;
}
