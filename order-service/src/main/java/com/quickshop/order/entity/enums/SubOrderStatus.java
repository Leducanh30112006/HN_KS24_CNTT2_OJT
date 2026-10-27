package com.quickshop.order.entity.enums;

/**
 * State machine cho Đơn con theo từng Shop (Sub-Order) theo SRS QuickShop 2.2.0:
 * CREATED -> CONFIRMED -> PACKING -> SHIPPING -> DELIVERED -> COMPLETED
 * Nhánh hoàn / hủy / khiếu nại: CANCELLED, DELIVERY_FAILED, REFUNDING, REFUNDED
 */
public enum SubOrderStatus {
    CREATED,
    CONFIRMED,
    PACKING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    DELIVERY_FAILED,
    REFUNDING,
    REFUNDED,
    CANCELLED
}
