package com.quickshop.order.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu hợp nhất Guest Cart vào User Cart sau khi đăng nhập (SRS QuickShop 2.2.0 - FR-05)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest implements Serializable {

    /**
     * Mã định danh yêu cầu chống lặp (Idempotency Key / Request ID)
     * Tránh cộng dồn 2 lần khi client retry do lỗi mạng hoặc nhấn nhiều lần.
     */
    private String requestId;

    @NotEmpty(message = "Danh sách mặt hàng từ Guest Cart không được để trống")
    @Valid
    @Builder.Default
    private List<GuestCartItemDto> items = new ArrayList<>();
}
