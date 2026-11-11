package com.quickshop.notification.ai.catalog;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe Circuit Breaker bảo vệ kết nối giữa AI Service và Catalog Service
 * Hỗ trợ các trạng thái: CLOSED, OPEN, HALF_OPEN
 */
@Slf4j
@Component
public class AiCatalogCircuitBreaker {

    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastFailureTimestamp = new AtomicLong(0);

    @Getter
    private final int failureThreshold;

    @Getter
    private final long resetTimeoutMs;

    public AiCatalogCircuitBreaker(
            @Value("${catalog.service.circuit-breaker.failure-threshold:3}") int failureThreshold,
            @Value("${catalog.service.circuit-breaker.reset-timeout-ms:30000}") long resetTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
    }

    /**
     * Kiểm tra xem request có được phép gửi đi hay không
     */
    public boolean allowRequest() {
        CircuitBreakerState currentState = state.get();

        if (currentState == CircuitBreakerState.CLOSED) {
            return true;
        }

        if (currentState == CircuitBreakerState.OPEN) {
            long now = System.currentTimeMillis();
            long lastFailure = lastFailureTimestamp.get();
            if (now - lastFailure >= resetTimeoutMs) {
                // Đã hết thời gian chờ ngắt mạch, chuyển sang HALF_OPEN để gửi request thăm dò
                if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                    log.info("[CIRCUIT_BREAKER] Chuyển trạng thái từ OPEN -> HALF_OPEN (Thử nghiệm thăm dò Catalog Service)");
                    return true;
                }
            }
            return false;
        }

        // HALF_OPEN: Cho phép request thăm dò tiếp tục
        return true;
    }

    public boolean allowExecution() {
        return allowRequest();
    }

    /**
     * Ghi nhận một lệnh gọi thành công
     */
    public void recordSuccess() {
        CircuitBreakerState prev = state.getAndSet(CircuitBreakerState.CLOSED);
        consecutiveFailures.set(0);
        if (prev != CircuitBreakerState.CLOSED) {
            log.info("[CIRCUIT_BREAKER] Catalog Service đã phục hồi thành công! Chuyển trạng thái {} -> CLOSED", prev);
        }
    }

    /**
     * Ghi nhận một lệnh gọi thất bại (Timeout, 5xx, Network Error)
     */
    public void recordFailure(String reason) {
        lastFailureTimestamp.set(System.currentTimeMillis());
        int currentFailures = consecutiveFailures.incrementAndGet();

        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            // Request thăm dò thất bại -> Quay lại trạng thái OPEN ngay lập tức
            state.set(CircuitBreakerState.OPEN);
            log.warn("[CIRCUIT_BREAKER] Request thăm dò thất bại ({}). Quay lại trạng thái OPEN trong {}ms", reason, resetTimeoutMs);
        } else if (currentFailures >= failureThreshold) {
            if (state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN)) {
                log.error("[CIRCUIT_BREAKER] Ngắt mạch OPEN! Đã đạt ngưỡng thất bại liên tiếp ({}/{}): {}",
                        currentFailures, failureThreshold, reason);
            }
        } else {
            log.warn("[CIRCUIT_BREAKER] Lỗi kết nối Catalog Service ({}/{}): {}", currentFailures, failureThreshold, reason);
        }
    }

    public void recordFailure() {
        recordFailure("Lỗi kết nối");
    }

    public int getFailureCount() {
        return consecutiveFailures.get();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("state", getState().name());
        status.put("failureCount", getConsecutiveFailures());
        status.put("failureThreshold", failureThreshold);
        status.put("resetTimeoutMs", resetTimeoutMs);
        status.put("lastFailureTimestamp", getLastFailureTimestamp());
        return status;
    }

    public CircuitBreakerState getState() {
        return state.get();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public long getLastFailureTimestamp() {
        return lastFailureTimestamp.get();
    }

    /**
     * Đặt lại trạng thái ban đầu (phục vụ Testing hoặc Admin Control)
     */
    public void reset() {
        state.set(CircuitBreakerState.CLOSED);
        consecutiveFailures.set(0);
        lastFailureTimestamp.set(0);
        log.info("[CIRCUIT_BREAKER] Đã reset về trạng thái CLOSED");
    }
}
