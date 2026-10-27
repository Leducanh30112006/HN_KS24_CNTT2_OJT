package com.quickshop.payment.dto;

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
public class WalletResponse {
    private Long id;
    private Long shopId;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private BigDecimal totalBalance;
    private LocalDateTime updatedAt;
}
