package com.quickshop.order.entity;

public enum MasterOrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PROCESSING,
    PARTIALLY_COMPLETED,
    COMPLETED,
    CANCELLED,
    REFUNDED
}
