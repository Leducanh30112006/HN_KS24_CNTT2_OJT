package com.quickshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEscrowRequest {

    @NotNull(message = "Sub-Order ID không được để trống")
    private Long subOrderId;

    @NotBlank(message = "Lý do tranh chấp/khiếu nại không được để trống")
    private String reason;
}
