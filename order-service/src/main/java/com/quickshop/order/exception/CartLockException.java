package com.quickshop.order.exception;

public class CartLockException extends RuntimeException {
    public CartLockException(String message) {
        super(message);
    }
}
