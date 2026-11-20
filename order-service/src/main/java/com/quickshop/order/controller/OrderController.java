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
import org.springframework.transaction.annotation.Transactional;
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

    @Operation(summary = "Lấy danh sách các đơn hàng của người dùng (kèm chi tiết SubOrder và OrderItems)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(value = "userId", required = false) Long paramUserId) {

        Long headerUserId = parseUserIdQuietly(userIdHeader);
        Long targetUserId;
        if (isAdmin(userRole)) {
            targetUserId = paramUserId != null ? paramUserId : headerUserId;
        } else {
            // Chống IDOR: Buyer/Seller/Shipper chỉ được xem đơn hàng của chính mình
            targetUserId = headerUserId != null ? headerUserId : paramUserId;
        }

        List<MasterOrder> masterOrders;
        if (targetUserId != null && targetUserId > 0) {
            masterOrders = masterOrderRepository.findByUserId(targetUserId);
        } else if (isAdmin(userRole)) {
            masterOrders = masterOrderRepository.findAll();
        } else {
            masterOrders = Collections.emptyList();
        }

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (MasterOrder mo : masterOrders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", mo.getId());
            map.put("orderCode", "QS-" + (100000 + mo.getId()));
            map.put("userId", mo.getUserId());
            map.put("status", mo.getStatus() != null ? mo.getStatus().name() : "PROCESSING");
            map.put("totalAmount", mo.getTotalAmount());
            map.put("discountAmount", mo.getVoucherDiscount());
            map.put("shippingFee", mo.getShippingFeeTotal());
            map.put("finalAmount", mo.getTotalAmount());
            map.put("paymentMethod", mo.getPaymentMethod());
            map.put("paymentStatus", mo.getStatus() == com.quickshop.order.entity.MasterOrderStatus.CANCELLED ? "CANCELLED" : "PAID");
            map.put("deliveryAddress", mo.getShippingAddress());
            map.put("createdAt", mo.getCreatedAt() != null ? mo.getCreatedAt().toString() : java.time.LocalDateTime.now().toString());

            List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(mo.getId());
            List<Map<String, Object>> subOrderDetails = new ArrayList<>();
            for (SubOrder sub : subOrders) {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", sub.getId());
                subMap.put("masterOrderId", sub.getMasterOrderId());
                subMap.put("shopId", sub.getShopId());
                subMap.put("shopName", "Shop #" + sub.getShopId() + " (Official Mall)");
                subMap.put("status", sub.getStatus() != null ? sub.getStatus().name() : "CONFIRMED");
                subMap.put("subTotal", sub.getSubtotalAmount());
                subMap.put("shippingFee", sub.getShippingFee());
                subMap.put("finalAmount", sub.getFinalAmount());
                subMap.put("carrierName", "Giao Hàng Nhanh (GHN)");
                subMap.put("trackingCode", "GHN-VNM-" + (100000 + sub.getId()));

                List<OrderItem> items = orderItemRepository.findBySubOrderId(sub.getId());
                subMap.put("items", items);
                subOrderDetails.add(subMap);
            }
            map.put("subOrders", subOrderDetails);
            responseList.add(map);
        }

        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đơn hàng thành công", responseList));
    }

    @Operation(summary = "Hủy đơn hàng nếu người bán chưa đóng gói hoặc vận chuyển")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestBody(required = false) Map<String, String> body) {

        Long rawId = id;
        if (id != null && id >= 100000) {
            rawId = id - 100000;
        }

        MasterOrder masterOrder = masterOrderRepository.findById(rawId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã ID #" + id));

        // Chống IDOR: chỉ chủ sở hữu đơn hàng hoặc Admin mới có quyền hủy đơn hàng
        Long requesterId = parseUserIdQuietly(userIdHeader);
        if (!isAdmin(userRole) && (requesterId == null || !requesterId.equals(masterOrder.getUserId()))) {
            log.warn("IDOR blocked on POST /api/orders/{}/cancel: requesterId={}, role={}, ownerId={}",
                    id, requesterId, userRole, masterOrder.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có quyền hủy đơn hàng của người dùng khác"));
        }

        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(rawId);
        boolean anyPackedOrShipped = subOrders.stream().anyMatch(sub ->
                sub.getStatus() == com.quickshop.order.entity.SubOrderStatus.PACKING ||
                sub.getStatus() == com.quickshop.order.entity.SubOrderStatus.SHIPPING ||
                sub.getStatus() == com.quickshop.order.entity.SubOrderStatus.DELIVERED ||
                sub.getStatus() == com.quickshop.order.entity.SubOrderStatus.COMPLETED
        );

        if (anyPackedOrShipped) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Không thể hủy đơn hàng do người bán đã đóng hàng hoặc bưu tá đang vận chuyển"));
        }

        masterOrder.setStatus(com.quickshop.order.entity.MasterOrderStatus.CANCELLED);
        masterOrderRepository.save(masterOrder);

        for (SubOrder sub : subOrders) {
            sub.setStatus(com.quickshop.order.entity.SubOrderStatus.CANCELLED);
            subOrderRepository.save(sub);
        }

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Khách hàng yêu cầu hủy đơn hàng";
        Map<String, Object> res = new HashMap<>();
        res.put("orderId", id);
        res.put("status", "CANCELLED");
        res.put("cancelReason", reason);

        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công", res));
    }

    @Operation(summary = "Lấy chi tiết đơn hàng tổng kèm các SubOrder và OrderItems snapshot")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetails(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        final Long effectiveRawId = (id != null && id >= 100000) ? (id - 100000) : id;

        MasterOrder masterOrder = masterOrderRepository.findById(effectiveRawId)
                .or(() -> (effectiveRawId.equals(id) ? Optional.empty() : masterOrderRepository.findById(id)))
                .or(() -> subOrderRepository.findById(effectiveRawId).flatMap(so -> masterOrderRepository.findById(so.getMasterOrderId())))
                .or(() -> (effectiveRawId.equals(id) ? Optional.empty() : subOrderRepository.findById(id).flatMap(so -> masterOrderRepository.findById(so.getMasterOrderId()))))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã ID #" + id));

        // Chống lỗ hổng IDOR: Buyer chỉ được xem đơn hàng thuộc quyền sở hữu của mình (Admin/Seller/Shipper được phép theo nghiệp vụ)
        Long requesterId = parseUserIdQuietly(userIdHeader);
        boolean isOwner = requesterId != null && requesterId.equals(masterOrder.getUserId());
        if (!isOwner && !isPrivilegedOrderViewer(userRole)) {
            log.warn("IDOR blocked on GET /api/orders/{}: requesterId={}, role={}, ownerId={}",
                    id, requesterId, userRole, masterOrder.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có quyền xem chi tiết đơn hàng của người dùng khác"));
        }

        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(masterOrder.getId());
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

    private Long parseUserIdQuietly(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isAdmin(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return false;
        }
        String r = userRole.trim().toUpperCase();
        return "ROLE_ADMIN".equals(r) || "ADMIN".equals(r);
    }

    private boolean isPrivilegedOrderViewer(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return false;
        }
        String r = userRole.trim().toUpperCase();
        return "ROLE_ADMIN".equals(r) || "ADMIN".equals(r)
                || "ROLE_SELLER".equals(r) || "SELLER".equals(r)
                || "ROLE_SHIPPER".equals(r) || "SHIPPER".equals(r);
    }
}
