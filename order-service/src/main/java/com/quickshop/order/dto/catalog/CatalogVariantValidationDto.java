package com.quickshop.order.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Contract DTO cần Catalog Service cung cấp qua REST API:
 * GET /api/catalog/variants/{variantId}/validation
 * Dùng để kiểm tra trạng thái hoạt động của Shop, Sản phẩm, Biến thể và Tồn kho khả dụng.
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
    private Integer stock;          // Tồn kho thực tế (stock)
    private Integer reservedStock;  // Tồn kho đang giữ thanh toán (reserved_stock)
    private Integer availableStock; // Tồn kho khả dụng = stock - reservedStock
    private boolean active;         // Trạng thái hoạt động (sản phẩm & shop đều ACTIVE)
}
