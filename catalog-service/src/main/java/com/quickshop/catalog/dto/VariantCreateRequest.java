package com.quickshop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantCreateRequest {
    @NotBlank(message = "Mã SKU không được để trống")
    private String sku;

    private String color;
    private String size;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Builder.Default
    private Integer stock = 0;
}
