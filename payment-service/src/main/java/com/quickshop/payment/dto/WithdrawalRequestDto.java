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
public class WithdrawalRequestDto {

    @NotNull(message = "Shop ID không được để trống")
    private Long shopId;

    @NotNull(message = "Số tiền rút không được để trống")
    @DecimalMin(value = "50000", message = "Số tiền rút tối thiểu là 50,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Mã ngân hàng không được để trống")
    private String bankCode;

    @NotBlank(message = "Số tài khoản ngân hàng không được để trống")
    private String bankAccountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String bankAccountName;

    private String idempotencyKey;
}
