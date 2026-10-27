package com.quickshop.payment.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferRequest {
    private Long withdrawalId;
    private Long shopId;
    private BigDecimal amount;
    private String bankCode;
    private String bankAccountNumber;
    private String bankAccountName;
}
