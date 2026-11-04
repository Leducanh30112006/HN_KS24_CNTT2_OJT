package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.order.dto.cart.*;
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
@Tag(name = "Cart Management", description = "APIs quản lý giỏ hàng phân tán đa gian hàng trên Redis (User Cart & Guest Cart)")
public class CartController {

    private final CartService cartService;

    // ========================================================
    // ENDPOINTS CHUNG (HỖ TRỢ CẢ USER VÀ GUEST QUA HEADER/PARAM)
    // ========================================================

    @Operation(summary = "Lấy giỏ hàng của người dùng hoặc khách vãng lai (Gom nhóm theo Shop)")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "User ID trích xuất từ JWT qua Gateway Header")
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @Parameter(description = "User ID query param")
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Parameter(description = "Guest ID trích xuất từ Header")
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @Parameter(description = "Guest ID query param")
            @RequestParam(value = "guestId", required = false) String paramGuestId) {

        Long userId = resolveUserIdQuietly(headerUserId, paramUserId);
        if (userId != null) {
            CartResponse cartResponse = cartService.getCart(userId);
            return ResponseEntity.ok(ApiResponse.ok("Lấy giỏ hàng thành công", cartResponse));
        }

        String guestId = resolveGuestId(headerGuestId, paramGuestId);
        CartResponse cartResponse = cartService.getGuestCart(guestId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy giỏ hàng khách vãng lai thành công", cartResponse));
    }

    @Operation(summary = "Thêm sản phẩm (SKU variant) vào giỏ hàng User hoặc Guest")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @RequestParam(value = "guestId", required = false) String paramGuestId,
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = resolveUserIdQuietly(headerUserId, paramUserId);
        if (userId != null) {
            CartResponse cartResponse = cartService.addToCart(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created("Thêm sản phẩm vào giỏ hàng thành công", cartResponse));
        }

        String guestId = resolveGuestId(headerGuestId, paramGuestId);
        CartResponse cartResponse = cartService.addToGuestCart(guestId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm sản phẩm vào giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Cập nhật số lượng của một mặt hàng trong giỏ")
    @PutMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @RequestParam(value = "guestId", required = false) String paramGuestId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        Long userId = resolveUserIdQuietly(headerUserId, paramUserId);
        if (userId != null) {
            CartResponse cartResponse = cartService.updateCartItemQuantity(userId, variantId, request);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật số lượng thành công", cartResponse));
        }

        String guestId = resolveGuestId(headerGuestId, paramGuestId);
        CartResponse cartResponse = cartService.updateGuestCartItemQuantity(guestId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số lượng giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Xóa một mặt hàng khỏi giỏ hàng")
    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @RequestParam(value = "guestId", required = false) String paramGuestId,
            @PathVariable("variantId") Long variantId) {

        Long userId = resolveUserIdQuietly(headerUserId, paramUserId);
        if (userId != null) {
            CartResponse cartResponse = cartService.removeCartItem(userId, variantId);
            return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm khỏi giỏ hàng thành công", cartResponse));
        }

        String guestId = resolveGuestId(headerGuestId, paramGuestId);
        CartResponse cartResponse = cartService.removeGuestCartItem(guestId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm khỏi giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Làm trống toàn bộ giỏ hàng")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @RequestParam(value = "guestId", required = false) String paramGuestId) {

        Long userId = resolveUserIdQuietly(headerUserId, paramUserId);
        if (userId != null) {
            cartService.clearCart(userId);
            return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng", null));
        }

        String guestId = resolveGuestId(headerGuestId, paramGuestId);
        cartService.clearGuestCart(guestId);
        return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng khách", null));
    }

    // ========================================================
    // ENDPOINTS CHUYÊN BIỆT CHO GUEST CART TRÊN REDIS
    // ========================================================

    @Operation(summary = "Lấy giỏ hàng khách vãng lai theo guestId (Redis TTL 7 ngày)")
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<ApiResponse<CartResponse>> getGuestCartDirect(@PathVariable("guestId") String guestId) {
        CartResponse cartResponse = cartService.getGuestCart(guestId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy giỏ hàng khách vãng lai thành công", cartResponse));
    }

    @Operation(summary = "Thêm sản phẩm vào giỏ hàng khách vãng lai theo guestId")
    @PostMapping("/guest/{guestId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToGuestCartDirect(
            @PathVariable("guestId") String guestId,
            @Valid @RequestBody AddToCartRequest request) {

        CartResponse cartResponse = cartService.addToGuestCart(guestId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm sản phẩm vào giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Cập nhật số lượng mặt hàng trong giỏ hàng khách vãng lai")
    @PutMapping("/guest/{guestId}/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateGuestCartDirect(
            @PathVariable("guestId") String guestId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse cartResponse = cartService.updateGuestCartItemQuantity(guestId, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số lượng giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Xóa mặt hàng khỏi giỏ hàng khách vãng lai")
    @DeleteMapping("/guest/{guestId}/items/{variantId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeGuestCartDirect(
            @PathVariable("guestId") String guestId,
            @PathVariable("variantId") Long variantId) {

        CartResponse cartResponse = cartService.removeGuestCartItem(guestId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa sản phẩm khỏi giỏ hàng khách thành công", cartResponse));
    }

    @Operation(summary = "Làm trống toàn bộ giỏ hàng khách vãng lai")
    @DeleteMapping("/guest/{guestId}")
    public ResponseEntity<ApiResponse<Void>> clearGuestCartDirect(@PathVariable("guestId") String guestId) {
        cartService.clearGuestCart(guestId);
        return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng khách", null));
    }

    // ========================================================
    // METADATA & MERGE CART
    // ========================================================

    @Operation(summary = "Tra cứu thông tin metadata và thời gian sống (TTL) của giỏ hàng trên Redis")
    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<CartMetadataDto>> getCartMetadata(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "guestId", required = false) String guestId) {

        if (userId != null && userId > 0) {
            CartMetadataDto metadata = cartService.getCartMetadata(CartType.USER, String.valueOf(userId));
            return ResponseEntity.ok(ApiResponse.ok("Tra cứu metadata giỏ hàng thành công", metadata));
        }

        if (guestId != null && !guestId.isBlank()) {
            CartMetadataDto metadata = cartService.getCartMetadata(CartType.GUEST, guestId.trim());
            return ResponseEntity.ok(ApiResponse.ok("Tra cứu metadata giỏ hàng thành công", metadata));
        }

        throw new BadRequestException("Yêu cầu cung cấp userId hoặc guestId để tra cứu metadata giỏ hàng");
    }

    @Operation(summary = "Hợp nhất danh sách mặt hàng từ Guest Cart vào User Cart sau khi đăng nhập")
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<MergeCartResponse>> mergeCart(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Guest-Id", required = false) String headerGuestId,
            @Parameter(description = "Idempotency Key chống gửi lặp qua Header")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody MergeCartRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);

        if (request == null || !request.hasValidPayload()) {
            if (headerGuestId != null && !headerGuestId.isBlank()) {
                if (request == null) {
                    request = MergeCartRequest.builder().guestId(headerGuestId.trim()).build();
                } else {
                    request.setGuestId(headerGuestId.trim());
                }
            } else {
                throw new BadRequestException("Yêu cầu hợp nhất giỏ hàng không hợp lệ: cần cung cấp guestId hoặc danh sách items");
            }
        } else if ((request.getGuestId() == null || request.getGuestId().isBlank())
                && headerGuestId != null && !headerGuestId.isBlank()) {
            request.setGuestId(headerGuestId.trim());
        }

        if (request.getRequestId() == null || request.getRequestId().isBlank()) {
            if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
                request.setRequestId(idempotencyKeyHeader.trim());
            }
        }
        MergeCartResponse response = cartService.mergeCart(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Hợp nhất giỏ hàng thành công", response));
    }

    @Operation(summary = "Hợp nhất toàn bộ giỏ hàng khách vãng lai trên Redis vào giỏ hàng người dùng khi đăng nhập")
    @PostMapping("/merge/guest/{guestId}")
    public ResponseEntity<ApiResponse<MergeCartResponse>> mergeGuestCartByPath(
            @PathVariable("guestId") String guestId,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        if (guestId == null || guestId.isBlank()) {
            throw new BadRequestException("Mã phiên khách vãng lai (guestId) không được để trống");
        }

        MergeCartResponse response = cartService.mergeGuestCartToUserCart(userId, guestId.trim(), idempotencyKeyHeader);
        return ResponseEntity.ok(ApiResponse.ok("Hợp nhất giỏ hàng khách vãng lai thành công", response));
    }

    private Long resolveUserId(Long headerUserId, Long paramUserId) {
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId");
        }
        return userId;
    }

    private Long resolveUserIdQuietly(Long headerUserId, Long paramUserId) {
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        return (userId != null && userId > 0) ? userId : null;
    }

    private String resolveGuestId(String headerGuestId, String paramGuestId) {
        String guestId = (headerGuestId != null && !headerGuestId.isBlank()) ? headerGuestId : paramGuestId;
        if (guestId == null || guestId.isBlank()) {
            throw new BadRequestException("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId");
        }
        return guestId.trim();
    }
}
