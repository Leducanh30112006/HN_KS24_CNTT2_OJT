package com.quickshop.order.dto.checkout;

import com.quickshop.order.entity.SubOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn hàng con tương ứng với từng Shop trong hệ thống Multi-Vendor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderResponse implements Serializable {

    private Long id;
    private String subOrderCode;
    private Long masterOrderId;
    private Long shopId;
    private String shopName;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal shopVoucherDiscount;
    private BigDecimal finalAmount;
    private SubOrderStatus status;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
}

