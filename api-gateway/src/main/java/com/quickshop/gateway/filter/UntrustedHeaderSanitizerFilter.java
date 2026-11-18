package com.quickshop.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Security Filter chạy ở độ ưu tiên cao nhất để loại bỏ toàn bộ header định danh
 * do client tự giả mạo (Header Spoofing: X-User-Id, X-User-Role, X-Auth-Username, X-User-Email)
 * trước khi chuyển tới các route/filter phía sau.
 * Chỉ có JwtAuthGatewayFilterFactory sau khi xác thực chữ ký JWT hợp lệ mới được phép gắn lại các header này.
 */
@Slf4j
@Component
public class UntrustedHeaderSanitizerFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Auth-Username");
                    headers.remove("X-User-Email");
                })
                .build();

        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
