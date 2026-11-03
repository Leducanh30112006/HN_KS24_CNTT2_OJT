package com.quickshop.catalog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotBlank(message = "Mã SKU không được để trống")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(length = 50)
    private String color;

    @Column(length = 20)
    private String size;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.0", message = "Giá bán không được nhỏ hơn 0")
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Builder.Default
    @Min(value = 0, message = "Số lượng tồn kho không được nhỏ hơn 0")
    @Column(nullable = false)
    private Integer stock = 0;

    @Builder.Default
    @Min(value = 0, message = "Tồn kho giữ chỗ không được nhỏ hơn 0")
    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    /**
     * Tính toán tồn kho khả dụng (Available Stock) cho khách hàng đặt mua:
     * Available = max(0, stock - reservedStock)
     */
    public int getAvailableStock() {
        int reserved = (reservedStock != null) ? reservedStock : 0;
        int currentStock = (stock != null) ? stock : 0;
        return Math.max(0, currentStock - reserved);
    }
}
