package com.quickshop.shipping.exception;

public class InvalidShippingStatusException extends RuntimeException {
    public InvalidShippingStatusException(String message) {
        super(message);
    }
}
