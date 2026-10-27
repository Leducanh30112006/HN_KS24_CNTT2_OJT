package com.quickshop.order.exception;

/**
 * Ngoại lệ ném ra khi không thể chiếm lock phân tán Redis (tránh xung đột đồng thời).
 */
public class CartLockException extends RuntimeException {

    public CartLockException(String message) {
        super(message);
    }
}
