package com.quickshop.order.util;

import com.quickshop.order.dto.cart.CartType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRedisKeyManagerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private CartRedisKeyManager keyManager;

    private static final Long USER_ID = 1001L;
    private static final String GUEST_ID = "guest-session-abc-123";

    @Test
    @DisplayName("Tạo key chuẩn cho User Cart và Lock Key")
    void testUserKeys() {
        assertEquals("cart:user:1001", keyManager.getUserCartKey(USER_ID));
        assertEquals("cart:1001", keyManager.getLegacyUserCartKey(USER_ID));
        assertEquals("lock:cart:user:1001", keyManager.getUserLockKey(USER_ID));
    }

    @Test
    @DisplayName("Tạo key chuẩn cho Guest Cart và Lock Key")
    void testGuestKeys() {
        assertEquals("cart:guest:guest-session-abc-123", keyManager.getGuestCartKey(GUEST_ID));
        assertEquals("lock:cart:guest:guest-session-abc-123", keyManager.getGuestLockKey(GUEST_ID));
    }

    @Test
    @DisplayName("Tạo key Guest rỗng hoặc null -> Báo lỗi IllegalArgumentException")
    void testGuestKey_Empty_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> keyManager.getGuestCartKey(null));
        assertThrows(IllegalArgumentException.class, () -> keyManager.getGuestCartKey("   "));
    }

    @Test
    @DisplayName("resolveUserCartKey khi có standard key -> trả về cart:user:{id}")
    void testResolveUserCartKey_HasStandard() {
        when(stringRedisTemplate.hasKey("cart:user:1001")).thenReturn(true);

        String resolved = keyManager.resolveUserCartKey(USER_ID);
        assertEquals("cart:user:1001", resolved);
    }

    @Test
    @DisplayName("resolveUserCartKey khi không có standard nhưng có legacy -> trả về cart:{id}")
    void testResolveUserCartKey_HasLegacy() {
        when(stringRedisTemplate.hasKey("cart:user:1001")).thenReturn(false);
        when(stringRedisTemplate.hasKey("cart:1001")).thenReturn(true);

        String resolved = keyManager.resolveUserCartKey(USER_ID);
        assertEquals("cart:1001", resolved);
    }

    @Test
    @DisplayName("resolveUserCartKey khi chưa có dữ liệu -> mặc định dùng standard key")
    void testResolveUserCartKey_DefaultStandard() {
        when(stringRedisTemplate.hasKey("cart:user:1001")).thenReturn(false);
        when(stringRedisTemplate.hasKey("cart:1001")).thenReturn(false);

        String resolved = keyManager.resolveUserCartKey(USER_ID);
        assertEquals("cart:user:1001", resolved);
    }

    @Test
    @DisplayName("Gia hạn TTL 30 ngày cho User Cart")
    void testRefreshTtl_UserCart() {
        keyManager.refreshTtl("cart:user:1001", CartType.USER);
        verify(stringRedisTemplate).expire("cart:user:1001", Duration.ofDays(30));
    }

    @Test
    @DisplayName("Gia hạn TTL 7 ngày cho Guest Cart")
    void testRefreshTtl_GuestCart() {
        keyManager.refreshTtl("cart:guest:abc", CartType.GUEST);
        verify(stringRedisTemplate).expire("cart:guest:abc", Duration.ofDays(7));
    }

    @Test
    @DisplayName("Đọc thời gian sống TTL còn lại từ Redis")
    void testGetTtlSeconds() {
        when(stringRedisTemplate.getExpire("cart:user:1001")).thenReturn(123456L);

        Long ttl = keyManager.getTtlSeconds("cart:user:1001");
        assertEquals(123456L, ttl);
    }
}

