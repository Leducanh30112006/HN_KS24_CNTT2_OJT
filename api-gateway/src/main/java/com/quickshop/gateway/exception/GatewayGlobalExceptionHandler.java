package com.quickshop.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-2)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Hệ thống tạm thời không khả dụng, vui lòng thử lại sau";

        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode statusCode = rse.getStatusCode();
            if (statusCode.equals(HttpStatus.UNAUTHORIZED)) {
                status = HttpStatus.UNAUTHORIZED;
                message = "Yêu cầu chưa được xác thực (401 Unauthorized)";
            } else if (statusCode.equals(HttpStatus.FORBIDDEN)) {
                status = HttpStatus.FORBIDDEN;
                message = "Bạn không có quyền truy cập tài nguyên này (403 Forbidden)";
            } else if (statusCode.equals(HttpStatus.NOT_FOUND)) {
                status = HttpStatus.NOT_FOUND;
                message = "Đường dẫn API không tồn tại hoặc đã thay đổi (404 Not Found)";
            } else if (statusCode.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Dịch vụ đích tạm thời không phản hồi, vui lòng thử lại sau (503 Service Unavailable)";
            } else if (statusCode.equals(HttpStatus.BAD_GATEWAY)) {
                status = HttpStatus.BAD_GATEWAY;
                message = "Không thể kết nối đến microservice xử lý yêu cầu (502 Bad Gateway)";
            } else if (statusCode instanceof HttpStatus hs) {
                status = hs;
                message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
            }
        }

        log.error("API Gateway intercepted error [{} {}] -> {}: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                status,
                ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Object> apiResponse = ApiResponse.error(message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(apiResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
