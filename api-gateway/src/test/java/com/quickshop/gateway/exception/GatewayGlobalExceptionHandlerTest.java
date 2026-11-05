package com.quickshop.gateway.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayGlobalExceptionHandler - Chuẩn hóa toàn bộ lỗi 401, 403, 404, 500 thành ApiResponse")
class GatewayGlobalExceptionHandlerTest {

    private GatewayGlobalExceptionHandler exceptionHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        exceptionHandler = new GatewayGlobalExceptionHandler();
    }

    @Test
    @DisplayName("Bắt lỗi 401 UNAUTHORIZED - Trả về mã 401 và body chuẩn ApiResponse tiếng Việt")
    void handle_UnauthorizedException_ReturnsStandardApiResponse() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).contains("401 Unauthorized");
    }

    @Test
    @DisplayName("Bắt lỗi 403 FORBIDDEN - Trả về mã 403 và body chuẩn ApiResponse tiếng Việt")
    void handle_ForbiddenException_ReturnsStandardApiResponse() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/admin").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).contains("403 Forbidden");
    }

    @Test
    @DisplayName("Bắt lỗi 404 NOT_FOUND - Trả về mã 404 và body chuẩn ApiResponse tiếng Việt")
    void handle_NotFoundException_ReturnsStandardApiResponse() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/non-existing").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).contains("404 Not Found");
    }

    @Test
    @DisplayName("Bắt lỗi 503 SERVICE_UNAVAILABLE khi microservice sập - Trả về mã 503 và body chuẩn")
    void handle_ServiceUnavailableException_ReturnsStandardApiResponse() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/catalog/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service Down");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).contains("503 Service Unavailable");
    }

    @Test
    @DisplayName("Bắt lỗi 500 INTERNAL_SERVER_ERROR thông thường - Trả về mã 500 chuẩn hóa")
    void handle_GenericException_ReturnsStandardApiResponse() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException ex = new RuntimeException("Unexpected gateway error");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).isNotNull();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).contains("Hệ thống tạm thời không khả dụng");
    }
}

