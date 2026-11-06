package com.quickshop.order.dto.voucher;

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
public class VoucherValidationResponse implements Serializable {

    private String code;
    private Long shopId;
    private boolean platformVoucher;
    private boolean valid;
    private String message;
    private String discountType;
    private BigDecimal discountValue;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
}
