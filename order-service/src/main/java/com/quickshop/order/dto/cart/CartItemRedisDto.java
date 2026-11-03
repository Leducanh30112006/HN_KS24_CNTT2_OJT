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
 * Cấu trúc dữ liệu lưu trữ dạng JSON trong Redis Hash key "cart:user:{id}" hoặc "cart:guest:{guestId}".
 * Giá và tồn kho tại đây đóng vai trò snapshot hiển thị trong giỏ hàng.
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
    private BigDecimal price; // Giá snapshot
    private Integer quantity;
    private Integer availableStock; // Tồn kho khả dụng snapshot

    @Builder.Default
    private CartType cartType = CartType.USER;

    private String ownerId; // userId hoặc guestId định danh chủ sở hữu item

    @Builder.Default
    private boolean selected = true; // Hỗ trợ đánh dấu chọn mua theo phân hệ Checkout

    private BigDecimal itemSubtotal; // Thành tiền snapshot = price * quantity

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
