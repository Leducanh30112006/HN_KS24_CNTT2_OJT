package com.quickshop.order.dto.cart;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu hợp nhất giỏ hàng khách vãng lai (Guest Cart) vào giỏ hàng người dùng (User Cart) sau khi đăng nhập.
 * Hỗ trợ hai nguồn dữ liệu:
 * 1. Từ phiên Redis Guest Cart (qua guestId)
 * 2. Từ danh sách mặt hàng lưu trữ tại LocalStorage của trình duyệt (qua items)
 * Hoặc kết hợp cả hai nguồn một cách mượt mà.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest implements Serializable {

    /**
     * Mã định danh yêu cầu chống gửi lặp (Idempotency Key).
     */
    private String requestId;

    /**
     * Mã phiên giỏ hàng khách vãng lai trên Redis (ví dụ: cart:guest:{guestId}).
     * Nếu có, hệ thống sẽ tự động đọc toàn bộ items từ Redis Guest Cart và hợp nhất.
     */
    private String guestId;

    /**
     * Tự động xóa Redis Guest Cart key sau khi hợp nhất thành công. Mặc định là true.
     */
    @Builder.Default
    private boolean clearGuestCartAfterMerge = true;

    /**
     * Danh sách mặt hàng khách vãng lai từ LocalStorage / Client payload.
     */
    @Valid
    @Builder.Default
    private List<GuestCartItemDto> items = new ArrayList<>();

    /**
     * Kiểm tra xem yêu cầu hợp nhất có chứa nguồn dữ liệu hợp lệ hay không.
     */
    public boolean hasValidPayload() {
        boolean hasGuestId = guestId != null && !guestId.trim().isEmpty();
        boolean hasItems = items != null && !items.isEmpty();
        return hasGuestId || hasItems;
    }
}
