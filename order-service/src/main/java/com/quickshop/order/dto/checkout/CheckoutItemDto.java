package com.quickshop.order.dto.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Thông tin mặt hàng được chọn để đặt hàng trong giỏ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemDto implements Serializable {

    @NotNull(message = "variantId không được để trống")
    @Positive(message = "variantId phải là số dương")
    private Long variantId;

    @NotNull(message = "quantity không được để trống")
    @Min(value = 1, message = "Số lượng đặt hàng tối thiểu là 1")
    private Integer quantity;
}

