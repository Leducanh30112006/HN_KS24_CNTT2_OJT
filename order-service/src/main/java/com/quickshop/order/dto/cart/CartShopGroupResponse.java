package com.quickshop.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Gom nhóm sản phẩm theo từng Shop theo yêu cầu QuickShop SRS (FR-05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartShopGroupResponse implements Serializable {

    private Long shopId;
    private String shopName;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private BigDecimal shopSubtotal;
    private Integer shopTotalItems;
}
