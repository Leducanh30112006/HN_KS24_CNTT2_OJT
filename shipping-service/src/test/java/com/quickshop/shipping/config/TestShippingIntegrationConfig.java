package com.quickshop.shipping.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestConfiguration
public class TestShippingIntegrationConfig {

    public static final Map<String, String> VALUE_STORE = new ConcurrentHashMap<>();
    public static final List<Object> PUBLISHED_MESSAGES = new CopyOnWriteArrayList<>();

    public static void clearAll() {
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
        return mock(ConnectionFactory.class);
    }

    @SuppressWarnings("unchecked")
    public static void configureRedisMock(StringRedisTemplate template) {
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

        when(template.opsForValue()).thenReturn(valOps);

        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            String key = arg != null ? arg.toString() : "";
            return VALUE_STORE.remove(key) != null;
        }).when(template).delete(any(String.class));

        doAnswer(invocation -> {
            Object arg = invocation.getArguments()[0];
            String key = arg != null ? arg.toString() : "";
            return VALUE_STORE.containsKey(key);
        }).when(template).hasKey(any(String.class));

        doAnswer(invocation -> true).when(template).expire(any(), any());
        doAnswer(invocation -> true).when(template).expire(any(), anyLong(), any());
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        configureRedisMock(template);
        return template;
    }
}
