package com.quickshop.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Bản ghi chi tiết từng mặt hàng trong đơn hàng phụ (SubOrder).
 * Lưu trữ snapshot toàn vẹn của tên sản phẩm, biến thể, mã SKU, giá bán và hình ảnh tại thời điểm thanh toán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false)
    private Long subOrderId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;
}
