package com.quickshop.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.dto.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Slf4j
@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private Key signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public String name() {
        return "JwtAuth";
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
        init();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                return onError(exchange, "Yêu cầu chưa được xác thực (Thiếu Token hoặc Cookie)", HttpStatus.UNAUTHORIZED);
            }

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Ngăn chặn sử dụng nhầm Refresh Token để gọi API được bảo vệ
                Object tokenType = claims.get("type");
                if ("REFRESH".equalsIgnoreCase(String.valueOf(tokenType))) {
                    log.warn("Access denied: provided token is a refresh token, not an access token");
                    return onError(exchange, "Token không hợp lệ (Không thể sử dụng Refresh Token để truy cập tài nguyên)", HttpStatus.UNAUTHORIZED);
                }

                String username = claims.getSubject();
                Object userId = claims.get("userId");
                Object email = claims.get("email");
                Object role = claims.get("role");
                String userRoleStr = role != null ? String.valueOf(role) : "";

                // Kiểm tra phân quyền RBAC nếu cấu hình requiredRole
                if (StringUtils.hasText(config.getRequiredRole()) && !config.getRequiredRole().equalsIgnoreCase(userRoleStr)) {
                    log.warn("Access denied: user role {} does not match required role {}", userRoleStr, config.getRequiredRole());
                    return onError(exchange, "Bạn không có quyền truy cập tài nguyên này", HttpStatus.FORBIDDEN);
                }

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-Username", username != null ? username : "")
                        .header("X-User-Id", userId != null ? String.valueOf(userId) : "")
                        .header("X-User-Email", email != null ? String.valueOf(email) : "")
                        .header("X-User-Role", userRoleStr)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException ex) {
                log.warn("JWT token expired: {}", ex.getMessage());
                return onError(exchange, "Token xác thực đã hết hạn, vui lòng làm mới token hoặc đăng nhập lại", HttpStatus.UNAUTHORIZED);
            } catch (SignatureException | MalformedJwtException ex) {
                log.warn("Invalid JWT signature or malformed token: {}", ex.getMessage());
                return onError(exchange, "Chữ ký hoặc định dạng Token không hợp lệ", HttpStatus.UNAUTHORIZED);
            } catch (Exception ex) {
                log.error("JWT token validation failed: {}", ex.getMessage());
                return onError(exchange, "Token xác thực không hợp lệ", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. Kiểm tra header Authorization: Bearer <token>
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Fallback trích xuất từ HttpOnly Cookie accessToken
        if (request.getCookies().containsKey("accessToken")) {
            HttpCookie cookie = request.getCookies().getFirst("accessToken");
            if (cookie != null && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Object> apiResponse = ApiResponse.error(errMessage);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(apiResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + errMessage + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Data
    public static class Config {
        private String requiredRole;
    }
}
