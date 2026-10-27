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
public class HoldEscrowRequest {

    @NotNull(message = "Sub-Order ID không được để trống")
    private Long subOrderId;

    @NotNull(message = "Master Order ID không được để trống")
    private Long masterOrderId;

    @NotNull(message = "Shop ID không được để trống")
    private Long shopId;

    @NotNull(message = "Số tiền tạm giữ không được để trống")
    @DecimalMin(value = "1", message = "Số tiền tạm giữ phải lớn hơn 0")
    private BigDecimal amount;
}
