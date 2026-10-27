package com.quickshop.order.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDistributedLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisDistributedLock redisDistributedLock;

    private static final String LOCK_KEY = "lock:cart:1001";
    private static final String LOCK_TOKEN = "valid-token-uuid";

    @BeforeEach
    void setUp() {
        redisDistributedLock = new RedisDistributedLock(stringRedisTemplate);
    }

    @Test
    @DisplayName("Chiếm khóa thành công ngay lần thử đầu tiên")
    void testAcquireLock_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        String token = redisDistributedLock.acquireLock(LOCK_KEY, 3000L, 3, 50L);

        assertNotNull(token);
        verify(valueOperations, times(1)).setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Không chiếm được khóa sau khi hết số lần retry -> Trả về null")
    void testAcquireLock_Failure_AfterRetries() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        String token = redisDistributedLock.acquireLock(LOCK_KEY, 3000L, 3, 10L);

        assertNull(token);
        verify(valueOperations, times(3)).setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Giải phóng khóa thành công khi token khớp")
    void testReleaseLock_Success() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList(LOCK_KEY)), eq(LOCK_TOKEN)))
                .thenReturn(1L);

        boolean released = redisDistributedLock.releaseLock(LOCK_KEY, LOCK_TOKEN);

        assertTrue(released);
        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), eq(Collections.singletonList(LOCK_KEY)), eq(LOCK_TOKEN));
    }

    @Test
    @DisplayName("Giải phóng khóa thất bại khi token không khớp hoặc đã hết hạn")
    void testReleaseLock_Failure_TokenMismatch() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList(LOCK_KEY)), eq(LOCK_TOKEN)))
                .thenReturn(0L);

        boolean released = redisDistributedLock.releaseLock(LOCK_KEY, LOCK_TOKEN);

        assertFalse(released);
    }

    @Test
    @DisplayName("Giải phóng khóa với tham số null -> Trả về false an toàn")
    void testReleaseLock_NullKeys_ReturnsFalse() {
        assertFalse(redisDistributedLock.releaseLock(null, LOCK_TOKEN));
        assertFalse(redisDistributedLock.releaseLock(LOCK_KEY, null));
        verifyNoInteractions(stringRedisTemplate);
    }
}
