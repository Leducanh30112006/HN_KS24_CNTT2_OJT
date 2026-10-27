package com.quickshop.order.entity.enums;

/**
 * State machine cho Đơn hàng tổng (Master Order) theo SRS QuickShop 2.2.0:
 * DRAFT -> PENDING_PAYMENT -> PROCESSING -> PARTIALLY_COMPLETED / COMPLETED / CANCELLED / REFUNDED
 */
public enum MasterOrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PROCESSING,
    PARTIALLY_COMPLETED,
    COMPLETED,
    CANCELLED,
    REFUNDED
}
