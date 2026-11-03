package com.quickshop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantUpdateRequest {
    private String sku;
    private String color;
    private String size;

    @DecimalMin(value = "0", message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Min(value = 0, message = "Số lượng tồn kho không được nhỏ hơn 0")
    private Integer stock;
}

