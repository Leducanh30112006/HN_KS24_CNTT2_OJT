package com.quickshop.catalog.dto;

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
public class VariantDto implements Serializable {
    private Long id;
    private Long productId;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer stock;
    private Integer reservedStock;
    private Integer availableStock;
}
