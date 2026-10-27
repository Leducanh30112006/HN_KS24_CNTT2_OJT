package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;
import com.quickshop.order.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs xử lý đặt hàng (Checkout), quản lý Master Order và Sub-Orders")
public class OrderController {

    private final CheckoutService checkoutService;

    @Operation(summary = "Tiến hành Checkout đơn hàng đa shop (Tạo Master Order và Sub-Orders)")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<MasterOrderResponse>> checkout(
            @Parameter(description = "User ID trích xuất từ JWT Gateway")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "User ID query param (fallback cho test)")
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Parameter(description = "Idempotency Key chống đặt trùng lặp đơn hàng")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CheckoutRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        MasterOrderResponse response = checkoutService.checkout(userId, idempotencyKeyHeader, request);

        if (response.isDuplicateRequest()) {
            return ResponseEntity.ok(ApiResponse.ok("Yêu cầu checkout lặp lại - trả về đơn hàng đã tạo trước đó", response));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đặt hàng thành công", response));
    }

    @Operation(summary = "Xem chi tiết Master Order và danh sách Sub-Orders theo ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<MasterOrderResponse>> getOrderById(
            @Parameter(description = "User ID trích xuất từ JWT Gateway")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "User ID query param (fallback cho test)")
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @PathVariable Long orderId) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        MasterOrderResponse response = checkoutService.getOrderById(userId, orderId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đơn hàng thành công", response));
    }

    private Long resolveUserId(Long headerUserId, Long paramUserId) {
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId");
        }
        return userId;
    }
}
