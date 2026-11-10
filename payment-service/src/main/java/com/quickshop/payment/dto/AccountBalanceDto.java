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
public class AccountBalanceDto {

    private String account;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal netBalance;
}
