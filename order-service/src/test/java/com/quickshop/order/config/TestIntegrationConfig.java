package com.quickshop.order.config;

import com.quickshop.order.util.RedisDistributedLock;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestConfiguration
public class TestIntegrationConfig {

    public static final Map<String, Map<Object, Object>> HASH_STORE = new ConcurrentHashMap<>();
    public static final Map<String, String> VALUE_STORE = new ConcurrentHashMap<>();
    public static final List<Object> PUBLISHED_MESSAGES = new CopyOnWriteArrayList<>();

    public static void clearAll() {
        HASH_STORE.clear();
        VALUE_STORE.clear();
        PUBLISHED_MESSAGES.clear();
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        return factory;
    }

    @Bean
    @Primary
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        org.springframework.amqp.rabbit.connection.Connection connection = mock(org.springframework.amqp.rabbit.connection.Connection.class);
        com.rabbitmq.client.Channel channel = mock(com.rabbitmq.client.Channel.class);
        try {
            when(connection.createChannel(anyBoolean())).thenReturn(channel);
            when(connection.isOpen()).thenReturn(true);
            when(factory.createConnection()).thenReturn(connection);
        } catch (Exception ignored) {}
        return factory;
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class));

        doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(template).convertAndSend(anyString(), anyString(), any(Object.class), any(org.springframework.amqp.core.MessagePostProcessor.class));

        return template;
    }

    @Bean
    @Primary
    public LockProvider lockProvider() {
        LockProvider provider = mock(LockProvider.class);
        SimpleLock simpleLock = mock(SimpleLock.class);
        when(provider.lock(any(LockConfiguration.class))).thenReturn(Optional.of(simpleLock));
        return provider;
    }

    @Bean
    @Primary
    public RedisDistributedLock redisDistributedLock() {
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        when(lock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(UUID.randomUUID().toString());
        when(lock.releaseLock(anyString(), anyString())).thenReturn(true);
        return lock;
    }

    public static void configureRedisMock(StringRedisTemplate template) {
        // Proxy cho HashOperations
        HashOperations<String, Object, Object> hashOps = (HashOperations<String, Object, Object>) Proxy.newProxyInstance(
                HashOperations.class.getClassLoader(),
                new Class<?>[]{HashOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("entries".equals(name)) {
                        String key = String.valueOf(args[0]);
                        return new HashMap<>(HASH_STORE.getOrDefault(key, Collections.emptyMap()));
                    }
                    if ("get".equals(name)) {
                        String key = String.valueOf(args[0]);
                        Map<Object, Object> m = HASH_STORE.get(key);
                        return m != null ? m.get(args[1]) : null;
                    }
                    if ("put".equals(name)) {
                        String key = String.valueOf(args[0]);
                        HASH_STORE.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(args[1], args[2]);
                        return null;
                    }
                    if ("putAll".equals(name)) {
                        String key = String.valueOf(args[0]);
                        Map<?, ?> m = (Map<?, ?>) args[1];
                        if (m != null) {
                            HASH_STORE.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).putAll(m);
                        }
                        return null;
                    }
                    if ("delete".equals(name)) {
                        String key = String.valueOf(args[0]);
                        Map<Object, Object> m = HASH_STORE.get(key);
                        if (m != null && args.length > 1) {
                            for (int i = 1; i < args.length; i++) {
                                m.remove(args[i]);
                            }
                        }
                        return 1L;
                    }
                    if ("size".equals(name)) {
                        String key = String.valueOf(args[0]);
                        return (long) HASH_STORE.getOrDefault(key, Collections.emptyMap()).size();
                    }
                    return null;
                }
        );

        // Proxy cho ValueOperations
        ValueOperations<String, String> valOps = (ValueOperations<String, String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class<?>[]{ValueOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("get".equals(name)) {
                        return VALUE_STORE.get(String.valueOf(args[0]));
                    }
                    if ("set".equals(name)) {
                        VALUE_STORE.put(String.valueOf(args[0]), String.valueOf(args[1]));
                        return null;
                    }
                    if ("setIfAbsent".equals(name)) {
                        String key = String.valueOf(args[0]);
                        String val = String.valueOf(args[1]);
                        if (!VALUE_STORE.containsKey(key)) {
                            VALUE_STORE.put(key, val);
                            return Boolean.TRUE;
                        }
                        return Boolean.FALSE;
                    }
                    return null;
                }
        );

        when(template.opsForHash()).thenReturn(hashOps);
        when(template.opsForValue()).thenReturn(valOps);

        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            String key = arg != null ? arg.toString() : "";
            return VALUE_STORE.remove(key) != null || HASH_STORE.remove(key) != null;
        }).when(template).delete(any(String.class));

        doAnswer(invocation -> {
            Collection<?> keys = (Collection<?>) invocation.getArguments()[0];
            if (keys != null) {
                for (Object k : keys) {
                    if (k != null) {
                        VALUE_STORE.remove(k.toString());
                        HASH_STORE.remove(k.toString());
                    }
                }
            }
            return 1L;
        }).when(template).delete(any(Collection.class));

        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            String key = arg != null ? arg.toString() : "";
            return VALUE_STORE.containsKey(key) || HASH_STORE.containsKey(key);
        }).when(template).hasKey(any(String.class));

        doAnswer(invocation -> true).when(template).expire(any(), any());
        doAnswer(invocation -> true).when(template).expire(any(), anyLong(), any());
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        configureRedisMock(template);
        return template;
    }
}
