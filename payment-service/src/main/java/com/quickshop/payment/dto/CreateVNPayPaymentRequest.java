package com.quickshop.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class CreateVNPayPaymentRequest {

    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String orderId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền thanh toán tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private Long userId;

    private String orderInfo;

    private String bankCode;
}
