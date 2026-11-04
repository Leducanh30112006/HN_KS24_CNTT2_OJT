package com.quickshop.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả xử lý hợp nhất giỏ hàng khách vãng lai vào giỏ hàng người dùng.
 * Bao gồm giỏ hàng mới nhất, danh sách cảnh báo tồn kho/SKU và các chỉ số đo lường chi tiết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartResponse implements Serializable {

    /**
     * Dữ liệu giỏ hàng người dùng hoàn chỉnh sau khi hợp nhất (đã gom nhóm theo Shop).
     */
    private CartResponse cart;

    /**
     * Danh sách cảnh báo chi tiết (về SKU trùng, hàng hết, hàng ngừng bán hoặc điều chỉnh tồn kho).
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Tổng số lượng mặt hàng (SKU) đã được hợp nhất thành công vào giỏ hàng.
     */
    private Integer mergedItemsCount;

    /**
     * Đánh dấu yêu cầu là yêu cầu gửi lặp được xử lý theo cơ chế Idempotent.
     */
    private boolean duplicateRequest;

    /**
     * Mã guestId đã được hợp nhất từ Redis (nếu có).
     */
    private String guestId;

    /**
     * Đánh dấu Redis Guest Cart key đã được xóa dọn dẹp sạch sẽ.
     */
    private boolean clearedGuestCart;

    /**
     * Số lượng SKU trùng lặp đã được cộng dồn số lượng vào User Cart.
     */
    @Builder.Default
    private Integer duplicateSkuCount = 0;

    /**
     * Số lượng mặt hàng bị điều chỉnh số lượng do vượt quá giới hạn tồn kho khả dụng.
     */
    @Builder.Default
    private Integer stockAdjustedCount = 0;
}
