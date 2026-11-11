package com.quickshop.notification.ai.catalog;

/**
 * Trạng thái của Circuit Breaker quản lý kết nối tới Catalog Service
 */
public enum CircuitBreakerState {
    /**
     * Mạch đóng: Hoạt động bình thường, tất cả request được chuyển tới Catalog Service
     */
    CLOSED,

    /**
     * Mạch ngắt: Đang gặp sự cố liên tiếp, lập tức kích hoạt Fallback không gửi request mạng
     */
    OPEN,

    /**
     * Bán mở (Thử nghiệm phục hồi): Cho phép 1 request thăm dò kiểm tra Catalog Service đã sẵn sàng
     */
    HALF_OPEN
}
