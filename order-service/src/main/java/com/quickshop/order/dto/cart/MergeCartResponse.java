package com.quickshop.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Phản hồi sau khi hợp nhất giỏ hàng.
 * Chứa giỏ hàng hoàn chỉnh đã cập nhật và danh sách cảnh báo (nếu có mặt hàng hết hàng hoặc điều chỉnh số lượng).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartResponse implements Serializable {

    private CartResponse cart;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private int mergedItemsCount;
    private boolean duplicateRequest;
}
