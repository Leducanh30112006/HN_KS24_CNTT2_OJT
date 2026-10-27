package com.quickshop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReport {
    private LocalDateTime reconciledAt;
    private String status; // "BALANCED" hoặc "DISCREPANCY"
    private boolean isDoubleEntryBalanced;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private String escrowStatus;
    private BigDecimal expectedEscrowHolding;
    private BigDecimal actualEscrowHolding;
    private BigDecimal escrowDiscrepancy;
    private List<String> details;
}
