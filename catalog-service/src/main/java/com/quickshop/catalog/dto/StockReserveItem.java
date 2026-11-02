package com.quickshop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveItem implements Serializable {
    private Long productId;
    private Long variantId;
    private String sku;
    private Integer quantity;
}
