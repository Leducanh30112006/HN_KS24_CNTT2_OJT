package com.quickshop.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAuditReportDto {

    private boolean isBalanced;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal discrepancy;
    private long totalEntries;
    private String status; // BALANCED, UNBALANCED
    private Map<String, BigDecimal> accountBalances;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime auditedAt = LocalDateTime.now();

    private String message;
}
