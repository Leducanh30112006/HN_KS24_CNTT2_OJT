package com.quickshop.order.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Khóa phân tán dựa trên Redis (Redis Distributed Lock)
 * Đảm bảo tính an toàn đồng thời (Concurrency Safety) khi nhiều request cùng thao tác vào giỏ hàng của 1 user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);

    /**
     * Cố gắng chiếm khóa với số lần thử lại tối đa.
     *
     * @param lockKey             Khóa định danh (vd: lock:cart:1001)
     * @param timeoutMillis       Thời gian giữ khóa tự hủy (tránh deadlock nếu node crash)
     * @param maxRetries          Số lần thử lại
     * @param retryIntervalMillis Khoảng cách giữa các lần thử lại (ms)
     * @return lockValue (UUID) nếu chiếm khóa thành công, null nếu thất bại
     */
    public String acquireLock(String lockKey, long timeoutMillis, int maxRetries, long retryIntervalMillis) {
        String lockValue = UUID.randomUUID().toString();
        for (int i = 0; i < maxRetries; i++) {
            try {
                Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                        lockKey,
                        lockValue,
                        Duration.ofMillis(timeoutMillis)
                );

                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("Đã chiếm thành công khóa [{}] với token [{}]", lockKey, lockValue);
                    return lockValue;
                }

                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Bị gián đoạn khi chờ acquire lock: {}", lockKey);
                break;
            } catch (Exception ex) {
                log.error("Lỗi khi acquire lock {}: {}", lockKey, ex.getMessage());
                break;
            }
        }
        return null;
    }

    /**
     * Giải phóng khóa an toàn bằng Lua script (chỉ xóa nếu đúng lockValue đã tạo).
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        if (lockKey == null || lockValue == null) {
            return false;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    unlockScript,
                    Collections.singletonList(lockKey),
                    lockValue
            );
            boolean released = result != null && result > 0;
            if (released) {
                log.debug("Đã giải phóng thành công khóa [{}]", lockKey);
            } else {
                log.warn("Không thể giải phóng khóa [{}] do token không khớp hoặc đã hết hạn", lockKey);
            }
            return released;
        } catch (Exception ex) {
            log.error("Lỗi khi release lock {}: {}", lockKey, ex.getMessage());
            return false;
        }
    }
}
