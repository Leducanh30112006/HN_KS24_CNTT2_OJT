package com.quickshop.order.dto.voucher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherApplyResponse implements Serializable {

    private VoucherValidationResponse platformVoucher;

    @Builder.Default
    private Map<Long, VoucherValidationResponse> shopVouchers = new HashMap<>();

    @Builder.Default
    private BigDecimal totalPlatformDiscount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalShopDiscount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;
}
