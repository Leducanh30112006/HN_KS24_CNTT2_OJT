package com.quickshop.order.util;

import com.quickshop.order.dto.cart.CartType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Quản lý định dạng khóa (Redis Keys) và thời gian sống (TTL) cho giỏ hàng phân tán.
 * Đảm bảo phân tách rõ ràng giữa User Cart và Guest Cart theo đặc tả kiến trúc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartRedisKeyManager {

    private final StringRedisTemplate stringRedisTemplate;

    public static final String USER_CART_PREFIX = "cart:user:";
    public static final String LEGACY_USER_CART_PREFIX = "cart:";
    public static final String GUEST_CART_PREFIX = "cart:guest:";

    public static final String USER_LOCK_PREFIX = "lock:cart:user:";
    public static final String GUEST_LOCK_PREFIX = "lock:cart:guest:";

    public static final Duration USER_CART_TTL = Duration.ofDays(30);
    public static final Duration GUEST_CART_TTL = Duration.ofDays(7);

    public String getUserCartKey(Long userId) {
        return USER_CART_PREFIX + userId;
    }

    public String getLegacyUserCartKey(Long userId) {
        return LEGACY_USER_CART_PREFIX + userId;
    }

    /**
     * Xác định Redis Cart Key cho User: ưu tiên key chuẩn `cart:user:{id}`,
     * hỗ trợ kiểm tra fallback `cart:{id}` nếu đã tồn tại trước đó để tương thích ngược.
     */
    public String resolveUserCartKey(Long userId) {
        String standardKey = getUserCartKey(userId);
        Boolean hasStandard = stringRedisTemplate.hasKey(standardKey);
        if (Boolean.TRUE.equals(hasStandard)) {
            return standardKey;
        }

        String legacyKey = getLegacyUserCartKey(userId);
        Boolean hasLegacy = stringRedisTemplate.hasKey(legacyKey);
        if (Boolean.TRUE.equals(hasLegacy)) {
            return legacyKey;
        }

        return standardKey;
    }

    public String getGuestCartKey(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new IllegalArgumentException("guestId không được để trống khi khởi tạo khóa Guest Cart");
        }
        return GUEST_CART_PREFIX + guestId.trim();
    }

    public String getUserLockKey(Long userId) {
        return USER_LOCK_PREFIX + userId;
    }

    public String getGuestLockKey(String guestId) {
        return GUEST_LOCK_PREFIX + (guestId != null ? guestId.trim() : "unknown");
    }

    /**
     * Gia hạn thời gian sống (TTL) cho giỏ hàng theo đúng phân loại CartType.
     */
    public void refreshTtl(String cartKey, CartType cartType) {
        Duration ttl = (cartType == CartType.GUEST) ? GUEST_CART_TTL : USER_CART_TTL;
        try {
            stringRedisTemplate.expire(cartKey, ttl);
            log.debug("Đã gia hạn TTL cho key [{}] với thời hạn {} ngày", cartKey, ttl.toDays());
        } catch (Exception ex) {
            log.warn("Không thể gia hạn TTL cho key {}: {}", cartKey, ex.getMessage());
        }
    }

    /**
     * Lấy thời gian sống còn lại (giây) của giỏ hàng trên Redis.
     */
    public Long getTtlSeconds(String cartKey) {
        try {
            return stringRedisTemplate.getExpire(cartKey);
        } catch (Exception ex) {
            log.warn("Lỗi khi đọc TTL cho key {}: {}", cartKey, ex.getMessage());
            return -1L;
        }
    }
}
