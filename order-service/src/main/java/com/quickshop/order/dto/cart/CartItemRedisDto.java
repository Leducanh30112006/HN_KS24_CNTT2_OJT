package com.quickshop.order.dto.cart;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data structure stored as JSON value inside Redis Hash key "cart:{user_id}".
 * Note: Price and stock stored here are purely snapshots for browsing in cart,
 * NOT official data for checkout (as required by QuickShop SRS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRedisDto implements Serializable {

    private Long variantId;
    private Long productId;
    private Long shopId;
    private String shopName;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private BigDecimal price; // Snapshot price
    private Integer quantity;
    private Integer availableStock; // Snapshot stock

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
