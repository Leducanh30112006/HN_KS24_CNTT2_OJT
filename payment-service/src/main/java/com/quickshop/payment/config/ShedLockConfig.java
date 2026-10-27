package com.quickshop.payment.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Cấu hình ShedLock đồng bộ Scheduled Tasks bằng Redis cho Payment Service theo SRS QuickShop 2.2.0 (FR-07.3).
 * Đảm bảo chỉ có duy nhất 1 container instance chạy tác vụ tại một thời điểm trong môi trường phân tán.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "quickshop-payment");
    }
}
