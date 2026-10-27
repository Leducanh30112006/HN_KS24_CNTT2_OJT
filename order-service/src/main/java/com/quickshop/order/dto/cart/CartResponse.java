package com.quickshop.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse implements Serializable {

    private Long userId;

    @Builder.Default
    private List<CartShopGroupResponse> shops = new ArrayList<>();

    private BigDecimal totalAmount;
    private Integer totalItems;      // Tổng số mặt hàng khác nhau trong giỏ
    private Integer totalQuantity;   // Tổng số lượng sản phẩm
}
