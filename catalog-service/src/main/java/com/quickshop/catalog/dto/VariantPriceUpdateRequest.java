package com.quickshop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class VariantPriceUpdateRequest {
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal price;
}

