package com.quickshop.order.exception;

public class CartOperationException extends RuntimeException {

    public CartOperationException(String message) {
        super(message);
    }

    public CartOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
