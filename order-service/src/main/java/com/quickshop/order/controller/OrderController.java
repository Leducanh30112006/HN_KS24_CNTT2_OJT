package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.checkout.CheckoutResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order & Checkout API", description = "Các điểm cuối xử lý thanh toán đa shop, tạo đơn hàng và tra cứu chi tiết đơn hàng")
public class OrderController {

    private final MasterOrderRepository masterOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CheckoutService checkoutService;

    @Operation(summary = "Tiến hành thanh toán và tạo đơn hàng MasterOrder & SubOrder theo từng Shop")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Valid @RequestBody CheckoutRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CheckoutResponse response = checkoutService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt hàng thành công", response));
    }

    @Operation(summary = "Xem trước thông tin thanh toán, phí ship và tổng tiền của đơn hàng trước khi đặt")
    @PostMapping("/checkout/preview")
    public ResponseEntity<ApiResponse<CheckoutResponse>> previewCheckout(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @RequestBody CheckoutRequest request) {

        Long userId = resolveUserId(headerUserId, paramUserId);
        CheckoutResponse response = checkoutService.previewCheckout(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Xem trước thông tin thanh toán thành công", response));
    }

    @Operation(summary = "Lấy danh sách các đơn hàng của người dùng")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MasterOrder>>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long userId = null;
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                userId = Long.parseLong(userIdHeader.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (userId == null) {
            userId = paramUserId;
        }

        if (userId != null && userId > 0) {
            return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đơn hàng thành công", masterOrderRepository.findByUserId(userId)));
        }
        return ResponseEntity.ok(ApiResponse.ok("Lấy toàn bộ đơn hàng thành công", masterOrderRepository.findAll()));
    }

    @Operation(summary = "Lấy chi tiết đơn hàng tổng kèm các SubOrder và OrderItems snapshot")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetails(@PathVariable Long id) {
        MasterOrder masterOrder = masterOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã ID #" + id));

        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(id);
        List<Map<String, Object>> subOrderDetailsList = new ArrayList<>();

        for (SubOrder subOrder : subOrders) {
            List<OrderItem> items = orderItemRepository.findBySubOrderId(subOrder.getId());
            Map<String, Object> subMap = new HashMap<>();
            subMap.put("subOrder", subOrder);
            subMap.put("items", items);
            subOrderDetailsList.add(subMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("masterOrder", masterOrder);
        result.put("subOrders", subOrderDetailsList);

        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết đơn hàng thành công", result));
    }

    @Operation(summary = "Lấy danh sách đơn hàng con thuộc về một Shop cụ thể")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<SubOrder>>> getShopSubOrders(@PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.ok("Lấy đơn hàng theo shop thành công", subOrderRepository.findByShopId(shopId)));
    }

    @Operation(summary = "Xác thực điều kiện đánh giá sản phẩm: SubOrder ở trạng thái COMPLETED và thuộc về người dùng")
    @GetMapping("/sub-orders/{subOrderId}/review-eligibility")
    public ResponseEntity<ApiResponse<Boolean>> checkReviewEligibility(
            @PathVariable Long subOrderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId
    ) {
        Optional<SubOrder> subOrderOpt = subOrderRepository.findById(subOrderId);
        if (subOrderOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("SubOrder không tồn tại", false));
        }

        SubOrder subOrder = subOrderOpt.get();

        // 1. Kiểm tra trạng thái đơn: phải là COMPLETED
        if (subOrder.getStatus() != com.quickshop.order.entity.SubOrderStatus.COMPLETED) {
            log.info("SubOrder #{} chưa hoàn thành (Trạng thái hiện tại: {})", subOrderId, subOrder.getStatus());
            return ResponseEntity.ok(ApiResponse.ok("Đơn hàng chưa hoàn thành (COMPLETED)", false));
        }

        // 2. Nếu truyền userId: kiểm tra đơn hàng có thuộc về user không qua MasterOrder
        if (userId != null && userId > 0) {
            Optional<MasterOrder> masterOpt = masterOrderRepository.findById(subOrder.getMasterOrderId());
            if (masterOpt.isPresent() && !masterOpt.get().getUserId().equals(userId)) {
                log.warn("User #{} không sở hữu đơn hàng Master #{}", userId, subOrder.getMasterOrderId());
                return ResponseEntity.ok(ApiResponse.ok("Đơn hàng không thuộc về người dùng", false));
            }
        }

        // 3. Nếu truyền productId: kiểm tra đơn hàng có chứa sản phẩm này không
        if (productId != null && productId > 0) {
            List<OrderItem> items = orderItemRepository.findBySubOrderId(subOrderId);
            boolean containsProduct = items.stream().anyMatch(item -> productId.equals(item.getProductId()));
            if (!containsProduct) {
                log.warn("SubOrder #{} không chứa sản phẩm #{}", subOrderId, productId);
                return ResponseEntity.ok(ApiResponse.ok("Đơn hàng không chứa sản phẩm này", false));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok("Đơn hàng đủ điều kiện đánh giá", true));
    }

    private Long resolveUserId(Long headerUserId, Long paramUserId) {
        Long userId = headerUserId != null ? headerUserId : paramUserId;
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Yêu cầu thông tin định danh người dùng qua X-User-Id header hoặc param userId");
        }
        return userId;
    }
}
