package com.quickshop.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RBAC Security Matrix - Ma trận kiểm thử phân quyền Guest, Buyer, Seller, Admin & Dual Auth")
class RbacSecurityMatrixTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private Key signingKey;
    private JwtAuthGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        filterFactory = new JwtAuthGatewayFilterFactory();
        filterFactory.setJwtSecret(SECRET);
    }

    private String createToken(String role) {
        return Jwts.builder()
                .setSubject("user_" + role.toLowerCase())
                .claim("userId", 101L)
                .claim("email", role.toLowerCase() + "@quickshop.vn")
                .claim("role", role)
                .claim("type", "ACCESS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Ma trận 1: Tài nguyên Admin (Duyệt Shop KYC) - Chỉ Admin được phép, từ chối Buyer và Seller với 403")
    void matrix_AdminOnlyEndpoint() {
        JwtAuthGatewayFilterFactory.Config adminConfig = new JwtAuthGatewayFilterFactory.Config();
        adminConfig.setRequiredRole("ROLE_ADMIN");
        GatewayFilter filter = filterFactory.apply(adminConfig);

        // 1. Guest -> 401 Unauthorized
        MockServerWebExchange guestEx = MockServerWebExchange.from(MockServerHttpRequest.put("/api/shops/1/approve").build());
        StepVerifier.create(filter.filter(guestEx, ex -> Mono.empty())).verifyComplete();
        assertThat(guestEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. Buyer -> 403 Forbidden
        MockServerWebExchange buyerEx = MockServerWebExchange.from(MockServerHttpRequest.put("/api/shops/1/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_BUYER")).build());
        StepVerifier.create(filter.filter(buyerEx, ex -> Mono.empty())).verifyComplete();
        assertThat(buyerEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 3. Seller -> 403 Forbidden
        MockServerWebExchange sellerEx = MockServerWebExchange.from(MockServerHttpRequest.put("/api/shops/1/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_SELLER")).build());
        StepVerifier.create(filter.filter(sellerEx, ex -> Mono.empty())).verifyComplete();
        assertThat(sellerEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 4. Admin -> 200 OK (Chuyển tiếp thành công)
        AtomicBoolean adminPassed = new AtomicBoolean(false);
        MockServerWebExchange adminEx = MockServerWebExchange.from(MockServerHttpRequest.put("/api/shops/1/approve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_ADMIN")).build());
        StepVerifier.create(filter.filter(adminEx, ex -> {
            adminPassed.set(true);
            return Mono.empty();
        })).verifyComplete();
        assertThat(adminPassed.get()).isTrue();
    }

    @Test
    @DisplayName("Ma trận 2: Tài nguyên Seller (Quản lý sản phẩm/đơn shop) - Cho phép Seller & Admin, từ chối Buyer với 403")
    void matrix_SellerAndAdminEndpoint() {
        JwtAuthGatewayFilterFactory.Config sellerConfig = new JwtAuthGatewayFilterFactory.Config();
        sellerConfig.setAllowedRoles("ROLE_SELLER,ROLE_ADMIN");
        GatewayFilter filter = filterFactory.apply(sellerConfig);

        // 1. Guest -> 401 Unauthorized
        MockServerWebExchange guestEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/catalog/products").build());
        StepVerifier.create(filter.filter(guestEx, ex -> Mono.empty())).verifyComplete();
        assertThat(guestEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. Buyer -> 403 Forbidden
        MockServerWebExchange buyerEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_BUYER")).build());
        StepVerifier.create(filter.filter(buyerEx, ex -> Mono.empty())).verifyComplete();
        assertThat(buyerEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 3. Seller -> Cho phép
        AtomicBoolean sellerPassed = new AtomicBoolean(false);
        MockServerWebExchange sellerEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_SELLER")).build());
        StepVerifier.create(filter.filter(sellerEx, ex -> {
            sellerPassed.set(true);
            return Mono.empty();
        })).verifyComplete();
        assertThat(sellerPassed.get()).isTrue();

        // 4. Admin -> Cho phép (Admin có toàn quyền quản trị)
        AtomicBoolean adminPassed = new AtomicBoolean(false);
        MockServerWebExchange adminEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/catalog/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("ROLE_ADMIN")).build());
        StepVerifier.create(filter.filter(adminEx, ex -> {
            adminPassed.set(true);
            return Mono.empty();
        })).verifyComplete();
        assertThat(adminPassed.get()).isTrue();
    }

    @ParameterizedTest(name = "Ma trận 3: Tài nguyên Người dùng chung - Role {0} được phép truy cập")
    @CsvSource({"ROLE_BUYER", "ROLE_SELLER", "ROLE_ADMIN"})
    @DisplayName("Ma trận 3: Tài nguyên Người dùng chung (Giỏ hàng, Đơn mua, Hồ sơ) - Buyer, Seller, Admin đều được truy cập")
    void matrix_GeneralUserEndpoint_AllRolesPermitted(String role) {
        JwtAuthGatewayFilterFactory.Config userConfig = new JwtAuthGatewayFilterFactory.Config();
        userConfig.setAllowedRoles("ROLE_BUYER,ROLE_SELLER,ROLE_ADMIN");
        GatewayFilter filter = filterFactory.apply(userConfig);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/my-orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(role)).build());

        StepVerifier.create(filter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(chainCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Ma trận 4: Dual Auth - Kiểm chứng đồng thời cơ chế Bearer Header và HttpOnly Cookie")
    void matrix_DualAuth_BearerAndCookieEquivalence() {
        JwtAuthGatewayFilterFactory.Config userConfig = new JwtAuthGatewayFilterFactory.Config();
        userConfig.setAllowedRoles("ROLE_BUYER,ROLE_SELLER,ROLE_ADMIN");
        GatewayFilter filter = filterFactory.apply(userConfig);

        String token = createToken("ROLE_BUYER");

        // Cách 1: Gửi qua Header Authorization
        AtomicBoolean bearerPassed = new AtomicBoolean(false);
        MockServerWebExchange bearerEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/cart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        StepVerifier.create(filter.filter(bearerEx, ex -> {
            bearerPassed.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_BUYER");
            return Mono.empty();
        })).verifyComplete();
        assertThat(bearerPassed.get()).isTrue();

        // Cách 2: Gửi qua HttpOnly Cookie accessToken
        AtomicBoolean cookiePassed = new AtomicBoolean(false);
        MockServerWebExchange cookieEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/cart")
                .cookie(new HttpCookie("accessToken", token)).build());
        StepVerifier.create(filter.filter(cookieEx, ex -> {
            cookiePassed.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_BUYER");
            return Mono.empty();
        })).verifyComplete();
        assertThat(cookiePassed.get()).isTrue();
    }
}
