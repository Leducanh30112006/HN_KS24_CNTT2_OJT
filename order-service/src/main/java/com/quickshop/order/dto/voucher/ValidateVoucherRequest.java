package com.quickshop.order.dto.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateVoucherRequest implements Serializable {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    private Long shopId;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @DecimalMin(value = "0", message = "Giá trị đơn hàng không được âm")
    private BigDecimal orderAmount;
}
