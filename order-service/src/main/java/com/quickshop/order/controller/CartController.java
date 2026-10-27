package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.order.dto.cart.AddToCartRequest;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.MergeCartRequest;
import com.quickshop.order.dto.cart.MergeCartResponse;
import com.quickshop.order.dto.cart.UpdateCartItemRequest;
import com.quickshop.order.service.CartService;
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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart Management", description = "APIs quản lý giỏ hàng phân tán đa gian hàng trên Redis")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Lấy giỏ hàng của người dùng hiện tại (Gom nhóm theo Shop)")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "User ID trích xuất từ JWT qua Gateway Header")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "User ID query param (fallback cho kiểm thử hoặc gọi nội bộ)")
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CartResponse cartResponse = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy giỏ hàng thành công", cartResponse));
    }

    @Operation(summary = "Thêm sản phẩm (SKU variant) vào giỏ hàng")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CartResponse cartResponse = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm sản phẩm vào giỏ hàng thành công", cartResponse));
    }

    @Operation(summary = "Cập nhật số lượng của một mặt hàng trong giỏ")
    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CartResponse cartResponse = cartService.updateCartItemQuantity(userId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số lượng thành công", cartResponse));
    }

    @Operation(summary = "Xóa một mặt hàng khỏi giỏ hàng")
    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @PathVariable("variantId") Long variantId) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CartResponse cartResponse = cartService.removeCartItem(userId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm khỏi giỏ hàng thành công", cartResponse));
    }

    @Operation(summary = "Làm trống toàn bộ giỏ hàng")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng", null));
    }

    @Operation(summary = "Hợp nhất danh sách mặt hàng từ Guest Cart vào User Cart sau khi đăng nhập")
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<MergeCartResponse>> mergeCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Parameter(description = "Idempotency Key chống gửi lặp qua Header")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody MergeCartRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        if (request.getRequestId() == null || request.getRequestId().isBlank()) {
            if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                request.setRequestId(idempotencyKeyHeader.trim());
            }
        }
        MergeCartResponse response = cartService.mergeCart(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Hợp nhất giỏ hàng thành công", response));
    }

    private Long resolveUserId(Long headerUserId, Long paramUserId) {
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId");
        }
        return userId;
    }
}
