package com.quickshop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalActionRequest {
    private String action; // APPROVE, REJECT, COMPLETE
    private Long adminId;
    private String reason;
}
