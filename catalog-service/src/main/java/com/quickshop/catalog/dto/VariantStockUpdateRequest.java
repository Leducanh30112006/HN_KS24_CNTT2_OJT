package com.quickshop.catalog.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantStockUpdateRequest {
    /**
     * Cập nhật số lượng tồn kho trực tiếp (newStock >= 0)
     */
    @Min(value = 0, message = "Số lượng tồn kho mới không được nhỏ hơn 0")
    private Integer newStock;

    /**
     * Hoặc tăng/giảm số lượng tồn kho tương đối (ví dụ: +15 hoặc -5)
     */
    private Integer quantityAdjustment;

    /**
     * Lý do điều chỉnh kho (Ví dụ: "Nhập hàng từ kho tổng", "Hàng hoàn trả", "Kiểm kê định kỳ")
     */
    private String reason;
}

