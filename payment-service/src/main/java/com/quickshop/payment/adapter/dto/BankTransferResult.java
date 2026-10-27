package com.quickshop.payment.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferResult {
    private boolean success;
    private String bankTransactionRef;
    private String responseCode;
    private String message;
    private LocalDateTime transferredAt;
}
