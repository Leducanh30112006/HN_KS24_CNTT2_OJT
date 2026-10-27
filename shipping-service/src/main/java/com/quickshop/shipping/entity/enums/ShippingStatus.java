package com.quickshop.shipping.entity.enums;

/**
 * Trạng thái vận đơn theo SRS QuickShop 2.2.0:
 * READY_TO_PICK -> PICKED -> DELIVERING -> DELIVERED
 * Nhánh lỗi/hoàn: DELIVERY_FAILED -> RETURNED
 */
public enum ShippingStatus {
    READY_TO_PICK,
    PICKED,
    DELIVERING,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNED;

    public boolean canTransitionTo(ShippingStatus next) {
        if (this == next) {
            return true;
        }
        return switch (this) {
            case READY_TO_PICK -> next == PICKED || next == DELIVERING || next == DELIVERY_FAILED || next == RETURNED;
            case PICKED -> next == DELIVERING || next == DELIVERY_FAILED || next == RETURNED;
            case DELIVERING -> next == DELIVERED || next == DELIVERY_FAILED || next == RETURNED;
            case DELIVERY_FAILED -> next == DELIVERING || next == RETURNED;
            case DELIVERED, RETURNED -> false; // Terminal states
        };
    }
}
