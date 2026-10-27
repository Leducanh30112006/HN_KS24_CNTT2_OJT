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
public class ApplyVoucherResult implements Serializable {

    private String code;
    private Long voucherId;
    private Long shopId;
    private BigDecimal discountAmount;
    private boolean applied;
}
