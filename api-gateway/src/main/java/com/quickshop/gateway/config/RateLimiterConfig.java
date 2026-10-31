package com.quickshop.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Ưu tiên đọc CF-Connecting-IP từ Cloudflare CDN/Proxy
            String ip = exchange.getRequest().getHeaders().getFirst("CF-Connecting-IP");
            if (!StringUtils.hasText(ip)) {
                ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            }
            if (!StringUtils.hasText(ip)) {
                ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
            } else {
                ip = ip.split(",")[0].trim();
            }
            return Mono.just(ip);
        };
    }
}
