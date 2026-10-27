package com.quickshop.order.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "variantId không được để trống")
    @Positive(message = "variantId phải là số dương")
    private Long variantId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "Số lượng thêm vào giỏ phải lớn hơn hoặc bằng 1")
    @Max(value = 999, message = "Số lượng cho mỗi mặt hàng không được vượt quá 999")
    private Integer quantity;
}
