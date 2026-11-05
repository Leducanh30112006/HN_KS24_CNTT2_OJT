package com.quickshop.order.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Snapshot chi tiết của mặt hàng trong đơn hàng tại thời điểm đặt mua.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse implements Serializable {

    private Long id;
    private Long subOrderId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}

