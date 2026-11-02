package com.quickshop.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO phục vụ xác thực biến thể SKU từ Order Service:
 * GET /api/catalog/variants/{variantId}/validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogVariantValidationDto implements Serializable {
    private Long variantId;
    private Long productId;
    private Long shopId;
    private String shopName;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private BigDecimal price;
    private Integer stock;
    private Integer reservedStock;
    private Integer availableStock;
    private boolean active;
}
