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
public class VoucherApplyRequest implements Serializable {

    /**
     * Mã voucher toàn sàn (nếu có)
     */
    private String platformVoucherCode;

    /**
     * Map các voucher của Shop: Map<shopId, voucherCode>
     */
    @Builder.Default
    private Map<Long, String> shopVouchers = new HashMap<>();

    /**
     * Map tiền hàng của từng Shop: Map<shopId, subtotal>
     */
    @Builder.Default
    private Map<Long, BigDecimal> shopSubtotals = new HashMap<>();
}
