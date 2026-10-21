package com.quickshop.common.event;

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
public class OrderItemEventDto implements Serializable {
    private Long productId;
    private Long variantId;
    private Long shopId;
    private String productName;
    private String variantName;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
}
