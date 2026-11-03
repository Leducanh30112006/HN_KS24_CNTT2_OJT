package com.quickshop.order.dto.cart;

import java.time.Duration;

/**
 * Định nghĩa phân loại giỏ hàng lưu trữ trên Redis và thời gian sống (TTL) tương ứng.
 * - USER: Giỏ hàng của người dùng đã xác thực (TTL 30 ngày)
 * - GUEST: Giỏ hàng tạm thời của khách vãng lai (TTL 7 ngày)
 */
public enum CartType {
    USER("user", Duration.ofDays(30)),
    GUEST("guest", Duration.ofDays(7));

    private final String prefix;
    private final Duration defaultTtl;

    CartType(String prefix, Duration defaultTtl) {
        this.prefix = prefix;
        this.defaultTtl = defaultTtl;
    }

    public String getPrefix() {
        return prefix;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }
}

