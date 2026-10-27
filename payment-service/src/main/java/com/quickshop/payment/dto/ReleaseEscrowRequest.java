package com.quickshop.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseEscrowRequest {

    @NotNull(message = "Sub-Order ID không được để trống")
    private Long subOrderId;

    @Builder.Default
    private String trigger = "BUYER_CONFIRMED";
}
