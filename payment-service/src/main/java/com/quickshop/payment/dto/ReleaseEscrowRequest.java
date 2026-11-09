package com.quickshop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseEscrowRequest {
    private Long subOrderId;
    private Long shopId;
    private String note;
}
