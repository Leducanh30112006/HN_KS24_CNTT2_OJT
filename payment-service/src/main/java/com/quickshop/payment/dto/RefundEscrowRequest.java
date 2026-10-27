package com.quickshop.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEscrowRequest {

    @NotNull(message = "Sub-Order ID không được để trống")
    private Long subOrderId;

    @NotNull(message = "Số tiền hoàn không được để trống")
    @DecimalMin(value = "1", message = "Số tiền hoàn phải lớn hơn 0")
    private BigDecimal refundAmount;

    private String reason;
}
