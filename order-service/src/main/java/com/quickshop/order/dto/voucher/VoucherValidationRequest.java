package com.quickshop.order.dto.voucher;

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
public class VoucherValidationRequest implements Serializable {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    /**
     * shopId của cửa hàng nếu là voucher shop; để null nếu là voucher toàn sàn
     */
    private Long shopId;

    /**
     * Tổng giá trị tiền hàng cần áp dụng voucher
     */
    @NotNull(message = "Giá trị đơn hàng không được để trống")
    private BigDecimal orderSubtotal;
}
