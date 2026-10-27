package com.quickshop.order.dto.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemRequest implements Serializable {

    @NotNull(message = "variantId không được để trống")
    private Long variantId;

    @Min(value = 1, message = "Số lượng đặt mua phải từ 1 trở lên")
    private Integer quantity;
}
