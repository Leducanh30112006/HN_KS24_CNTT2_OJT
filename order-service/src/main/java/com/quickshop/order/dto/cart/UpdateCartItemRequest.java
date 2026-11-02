package com.quickshop.order.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {

    @NotNull(message = "quantity không được để trống")
    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0 (0 đồng nghĩa xóa mặt hàng)")
    @Max(value = 999, message = "Số lượng cho mỗi mặt hàng không được vượt quá 999")
    private Integer quantity;
}
