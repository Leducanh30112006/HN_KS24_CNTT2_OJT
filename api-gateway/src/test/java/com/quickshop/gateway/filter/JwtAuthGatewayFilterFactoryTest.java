package com.quickshop.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtAuthGatewayFilterFactory - Gateway JWT Middleware & Chuẩn hóa lỗi 401/403")
class JwtAuthGatewayFilterFactoryTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private Key signingKey;
    private JwtAuthGatewayFilterFactory filterFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        filterFactory = new JwtAuthGatewayFilterFactory();
        filterFactory.setJwtSecret(SECRET);
    }

    private String generateToken(String username, Long userId, String email, String role, String type, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Thiếu Token hoặc Cookie - Trả về 401 UNAUTHORIZED và chuẩn hóa ApiResponse")
    void apply_MissingToken_Returns401Unauthorized() throws Exception {
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/my-orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String responseBody = exchange.getResponse().getBodyAsString().block();
        assertThat(responseBody).isNotNull();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("message").asText()).contains("Yêu cầu chưa được xác thực");
    }

    @Test
    @DisplayName("Bearer Token hợp lệ - Chuyển tiếp thành công và mutate headers định danh người dùng")
    void apply_ValidBearerToken_PassesAndMutatesHeaders() {
        String token = generateToken("buyer1", 100L, "buyer1@quickshop.vn", "ROLE_BUYER", "ACCESS", 3600000L);

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/my-orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> passedExchange = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            passedExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled.get()).isTrue();
        ServerWebExchange mutated = passedExchange.get();
        assertThat(mutated.getRequest().getHeaders().getFirst("X-Auth-Username")).isEqualTo("buyer1");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("100");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("buyer1@quickshop.vn");
        assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_BUYER");
    }

    @Test
    @DisplayName("Cookie accessToken hợp lệ - Dual Auth tự động nhận diện cookie và chuyển tiếp thành công")
    void apply_ValidCookieToken_PassesAndMutatesHeaders() {
        String token = generateToken("seller1", 200L, "seller1@quickshop.vn", "ROLE_SELLER", "ACCESS", 3600000L);

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/shops/my-shop")
                .cookie(new HttpCookie("accessToken", token))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-Auth-Username")).isEqualTo("seller1");
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_SELLER");
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Token đã hết hạn - Trả về 401 UNAUTHORIZED thông báo Token hết hạn")
    void apply_ExpiredToken_Returns401Unauthorized() throws Exception {
        String expiredToken = generateToken("buyer1", 100L, "buyer1@quickshop.vn", "ROLE_BUYER", "ACCESS", -10000L);

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String responseBody = exchange.getResponse().getBodyAsString().block();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("message").asText()).contains("Token xác thực đã hết hạn");
    }

    @Test
    @DisplayName("Token giả mạo hoặc sai chữ ký - Trả về 401 UNAUTHORIZED")
    void apply_MalformedToken_Returns401Unauthorized() throws Exception {
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String responseBody = exchange.getResponse().getBodyAsString().block();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("message").asText()).contains("Chữ ký hoặc định dạng Token không hợp lệ");
    }

    @Test
    @DisplayName("Sử dụng nhầm Refresh Token - Trả về 401 UNAUTHORIZED từ chối Refresh Token")
    void apply_RefreshTokenUsedAsAccessToken_Returns401Unauthorized() throws Exception {
        String refreshToken = generateToken("buyer1", 100L, "buyer1@quickshop.vn", "ROLE_BUYER", "REFRESH", 3600000L);

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String responseBody = exchange.getResponse().getBodyAsString().block();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("message").asText()).contains("Không thể sử dụng Refresh Token để truy cập tài nguyên");
    }

    @Test
    @DisplayName("Phân quyền RBAC không khớp - Trả về 403 FORBIDDEN chuẩn hóa")
    void apply_RoleMismatch_Returns403Forbidden() throws Exception {
        String buyerToken = generateToken("buyer1", 100L, "buyer1@quickshop.vn", "ROLE_BUYER", "ACCESS", 3600000L);

        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        config.setRequiredRole("ROLE_ADMIN");

        GatewayFilter filter = filterFactory.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin/system")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        String responseBody = exchange.getResponse().getBodyAsString().block();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        assertThat(rootNode.get("success").asBoolean()).isFalse();
        assertThat(rootNode.get("message").asText()).contains("Bạn không có quyền truy cập tài nguyên này");
    }

    @Test
    @DisplayName("Phân quyền RBAC khớp - Cho phép đi tiếp vào microservice đích")
    void apply_RoleMatches_PassesToService() {
        String adminToken = generateToken("admin", 1L, "admin@quickshop.vn", "ROLE_ADMIN", "ACCESS", 3600000L);

        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        config.setRequiredRole("ROLE_ADMIN");

        GatewayFilter filter = filterFactory.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin/system")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(chainCalled.get()).isTrue();
    }
}
