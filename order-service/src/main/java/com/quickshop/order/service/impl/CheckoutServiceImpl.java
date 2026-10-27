package com.quickshop.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.CartItemRedisDto;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.dto.checkout.CheckoutItemRequest;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;
import com.quickshop.order.dto.order.OrderItemResponse;
import com.quickshop.order.dto.order.SubOrderResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.exception.CartLockException;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.saga.OrderSagaCoordinator;
import com.quickshop.order.service.CartService;
import com.quickshop.order.service.CheckoutService;
import com.quickshop.order.service.VoucherService;
import com.quickshop.order.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final MasterOrderRepository masterOrderRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final CatalogServiceClient catalogServiceClient;
    private final CartService cartService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisDistributedLock redisDistributedLock;
    private final OrderSagaCoordinator orderSagaCoordinator;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final String LOCK_KEY_PREFIX = "lock:checkout:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "checkout:idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);

    // Distributed lock configuration
    private static final long LOCK_TIMEOUT_MS = 5000L;
    private static final int LOCK_MAX_RETRIES = 5;
    private static final long LOCK_RETRY_INTERVAL_MS = 100L;

    // Standard default shipping fee per shop (30,000 VND)
    private static final BigDecimal DEFAULT_SHIPPING_FEE_PER_SHOP = BigDecimal.valueOf(30000);

    @Override
    @Transactional
    public MasterOrderResponse checkout(Long userId, String idempotencyKeyHeader, CheckoutRequest request) {
        // 1. Kiểm tra người dùng, địa chỉ giao hàng và phương thức thanh toán
        validateUser(userId);
        validateCheckoutRequest(request);

        String effectiveRequestId = resolveEffectiveRequestId(idempotencyKeyHeader, request);
        String idempotencyKey = effectiveRequestId != null ? IDEMPOTENCY_KEY_PREFIX + userId + ":" + effectiveRequestId : null;

        // 2. Kiểm tra Idempotency trước khi lấy lock (fast-path)
        if (idempotencyKey != null) {
            String existingOrderIdStr = stringRedisTemplate.opsForValue().get(idempotencyKey);
            if (existingOrderIdStr != null) {
                log.info("Phát hiện request checkout lặp (Idempotency Key: {}), trả về đơn hàng đã tạo ID: {}",
                        effectiveRequestId, existingOrderIdStr);
                return getExistingOrderResponse(Long.parseLong(existingOrderIdStr), userId, true);
            }
        }

        // 3. Acquire Distributed Lock để ngăn chặn race condition / double checkout cùng lúc
        String lockKey = LOCK_KEY_PREFIX + userId;
        String lockToken = redisDistributedLock.acquireLock(lockKey, LOCK_TIMEOUT_MS, LOCK_MAX_RETRIES, LOCK_RETRY_INTERVAL_MS);
        if (lockToken == null) {
            log.warn("Không thể lấy distributed lock cho checkout của user {}", userId);
            throw new CartLockException("Hệ thống đang xử lý một giao dịch đặt hàng khác của bạn, vui lòng đợi trong giây lát.");
        }

        try {
            // Double-check idempotency sau khi có lock
            if (idempotencyKey != null) {
                String existingOrderIdStr = stringRedisTemplate.opsForValue().get(idempotencyKey);
                if (existingOrderIdStr != null) {
                    log.info("Double-check phát hiện request checkout lặp (Idempotency Key: {}), trả về đơn hàng ID: {}",
                            effectiveRequestId, existingOrderIdStr);
                    return getExistingOrderResponse(Long.parseLong(existingOrderIdStr), userId, true);
                }
            }

            // 4. Xác định danh sách mặt hàng checkout (từ request hoặc từ Redis Cart)
            Map<Long, Integer> itemsToCheckout = resolveItemsToCheckout(userId, request);
            if (itemsToCheckout.isEmpty()) {
                throw new BadRequestException("Danh sách sản phẩm checkout không được để trống hoặc giỏ hàng đang trống.");
            }

            // 5. Xác minh từng sản phẩm với Catalog Service và tạo snapshot giá/tồn kho
            List<ResolvedCheckoutItem> resolvedItems = verifyAndSnapshotCatalogItems(itemsToCheckout);

            // 6. Tách thành các Sub-Orders theo từng shop (Gom nhóm theo shopId)
            Map<Long, List<ResolvedCheckoutItem>> itemsByShop = resolvedItems.stream()
                    .collect(Collectors.groupingBy(ResolvedCheckoutItem::getShopId));

            List<SubOrder> subOrders = new ArrayList<>();
            BigDecimal totalMerchandiseAmount = BigDecimal.ZERO;
            BigDecimal shippingFeeTotal = BigDecimal.ZERO;
            List<String> appliedShopVoucherCodes = new ArrayList<>();
            String appliedPlatformVoucherCode = null;

            try {
                for (Map.Entry<Long, List<ResolvedCheckoutItem>> shopEntry : itemsByShop.entrySet()) {
                    Long shopId = shopEntry.getKey();
                    List<ResolvedCheckoutItem> shopItems = shopEntry.getValue();

                    // Tính subtotal của Shop
                    BigDecimal shopSubtotal = shopItems.stream()
                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Cước vận chuyển của Shop
                    BigDecimal shopShippingFee = DEFAULT_SHIPPING_FEE_PER_SHOP;

                    // Tính voucher riêng của Shop qua VoucherService (nguyên tử và kiểm tra hợp lệ)
                    String shopVoucherCode = request.getShopVoucherCodes() != null ? request.getShopVoucherCodes().get(shopId) : null;
                    BigDecimal shopVoucherDiscount = BigDecimal.ZERO;
                    String finalShopVoucherCode = null;

                    if (shopVoucherCode != null && !shopVoucherCode.trim().isEmpty()) {
                        com.quickshop.order.dto.voucher.ApplyVoucherResult shopVoucherResult =
                                voucherService.applyShopVoucher(shopVoucherCode, shopId, shopSubtotal);
                        if (shopVoucherResult.isApplied()) {
                            shopVoucherDiscount = shopVoucherResult.getDiscountAmount();
                            finalShopVoucherCode = shopVoucherResult.getCode();
                            appliedShopVoucherCodes.add(finalShopVoucherCode);
                        }
                    }

                    // final_amount của Sub-Order = subtotal + shipping_fee - shop_voucher_discount
                    BigDecimal shopFinalAmount = shopSubtotal.add(shopShippingFee).subtract(shopVoucherDiscount);
                    if (shopFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
                        shopFinalAmount = BigDecimal.ZERO;
                    }

                    SubOrder subOrder = SubOrder.builder()
                            .shopId(shopId)
                            .subtotalAmount(shopSubtotal)
                            .shippingFee(shopShippingFee)
                            .shopVoucherDiscount(shopVoucherDiscount)
                            .shopVoucherCode(finalShopVoucherCode)
                            .finalAmount(shopFinalAmount)
                            .status(SubOrderStatus.CREATED)
                            .items(new ArrayList<>())
                            .build();

                    // Tạo order_items với snapshot thông tin sản phẩm và giá
                    for (ResolvedCheckoutItem resolvedItem : shopItems) {
                        OrderItem orderItem = OrderItem.builder()
                                .variantId(resolvedItem.getVariantId())
                                .productName(resolvedItem.getProductName())
                                .variantName(resolvedItem.getVariantName())
                                .imageUrl(resolvedItem.getImageUrl())
                                .price(resolvedItem.getPrice())
                                .quantity(resolvedItem.getQuantity())
                                .build();

                        subOrder.addItem(orderItem);
                    }

                    subOrders.add(subOrder);
                    totalMerchandiseAmount = totalMerchandiseAmount.add(shopSubtotal);
                    shippingFeeTotal = shippingFeeTotal.add(shopShippingFee);
                }

                // 7. Tính Voucher toàn sàn (Platform Voucher) qua VoucherService
                BigDecimal platformVoucherDiscount = BigDecimal.ZERO;
                if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
                    com.quickshop.order.dto.voucher.ApplyVoucherResult platformResult =
                            voucherService.applyPlatformVoucher(request.getVoucherCode(), totalMerchandiseAmount);
                    if (platformResult.isApplied()) {
                        platformVoucherDiscount = platformResult.getDiscountAmount();
                        appliedPlatformVoucherCode = platformResult.getCode();
                    }
                }

                // 8. Tính tổng thanh toán Master Order = sum(sub_orders.final_amount) - voucher_discount
                BigDecimal totalSubOrdersFinal = subOrders.stream()
                        .map(SubOrder::getFinalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal masterTotalAmount = totalSubOrdersFinal.subtract(platformVoucherDiscount);
                if (masterTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
                    masterTotalAmount = BigDecimal.ZERO;
                }

                // 9. Tạo Master Order và liên kết các Sub-Orders
                MasterOrder masterOrder = MasterOrder.builder()
                        .userId(userId)
                        .totalAmount(masterTotalAmount)
                        .voucherDiscount(platformVoucherDiscount)
                        .voucherCode(appliedPlatformVoucherCode)
                        .shippingFeeTotal(shippingFeeTotal)
                        .status(MasterOrderStatus.PENDING_PAYMENT)
                        .paymentMethod(request.getPaymentMethod())
                        .shippingAddress(request.getShippingAddress().trim())
                        .recipientName(request.getRecipientName())
                        .recipientPhone(request.getRecipientPhone())
                        .subOrders(new ArrayList<>())
                        .build();

                for (SubOrder subOrder : subOrders) {
                    masterOrder.addSubOrder(subOrder);
                }

                // 10. Lưu dữ liệu vào order_db bằng JPA Transaction
                MasterOrder savedMasterOrder = masterOrderRepository.save(masterOrder);
                log.info("Tạo Master Order ID: {} thành công với {} Sub-Orders cho user {}",
                        savedMasterOrder.getId(), savedMasterOrder.getSubOrders().size(), userId);

                // 11. Chỉ xóa các mặt hàng đã checkout thành công khỏi Redis Cart
                List<Long> checkedOutVariantIds = new ArrayList<>(itemsToCheckout.keySet());
                try {
                    cartService.removeCartItems(userId, checkedOutVariantIds);
                } catch (Exception ex) {
                    log.warn("Không thể xóa các variant {} khỏi giỏ hàng Redis của user {}: {}",
                            checkedOutVariantIds, userId, ex.getMessage());
                }

                // 12. Ghi nhận Idempotency Token vào Redis (TTL 30 phút)
                if (idempotencyKey != null) {
                    stringRedisTemplate.opsForValue().set(idempotencyKey, String.valueOf(savedMasterOrder.getId()), IDEMPOTENCY_TTL);
                }

                // 13. Điểm tích hợp Saga Orchestrator (cho task tiếp theo)
                orderSagaCoordinator.initiateCheckoutSaga(savedMasterOrder);

                // 14. Trả về Master Order và danh sách Sub-Orders
                return mapToMasterOrderResponse(savedMasterOrder, false);

            } catch (Exception ex) {
                // Tự động hoàn tác lượt sử dụng voucher nếu xảy ra lỗi trong quá trình tạo đơn
                if (appliedPlatformVoucherCode != null || !appliedShopVoucherCodes.isEmpty()) {
                    log.warn("Lỗi trong quá trình checkout, đang kích hoạt hoàn tác lượt dùng voucher: platform={}, shops={}",
                            appliedPlatformVoucherCode, appliedShopVoucherCodes);
                    voucherService.releaseVouchers(appliedPlatformVoucherCode, appliedShopVoucherCodes);
                }
                throw ex;
            }

        } finally {
            redisDistributedLock.releaseLock(lockKey, lockToken);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MasterOrderResponse getOrderById(Long userId, Long masterOrderId) {
        validateUser(userId);
        MasterOrder masterOrder = masterOrderRepository.findById(masterOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với ID: " + masterOrderId));

        if (!masterOrder.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem thông tin đơn hàng này.");
        }

        return mapToMasterOrderResponse(masterOrder, false);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("User ID không hợp lệ hoặc thiếu thông tin xác thực.");
        }
    }

    private void validateCheckoutRequest(CheckoutRequest request) {
        if (request == null) {
            throw new BadRequestException("Yêu cầu checkout không được để trống.");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().trim().isEmpty()) {
            throw new BadRequestException("Địa chỉ nhận hàng không được để trống.");
        }
        if (request.getPaymentMethod() == null) {
            throw new BadRequestException("Phương thức thanh toán không được để trống.");
        }
    }

    private String resolveEffectiveRequestId(String idempotencyKeyHeader, CheckoutRequest request) {
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.trim().isEmpty()) {
            return idempotencyKeyHeader.trim();
        }
        if (request.getRequestId() != null && !request.getRequestId().trim().isEmpty()) {
            return request.getRequestId().trim();
        }
        return null;
    }

    private MasterOrderResponse getExistingOrderResponse(Long orderId, Long userId, boolean isDuplicate) {
        MasterOrder existingOrder = masterOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại: " + orderId));
        return mapToMasterOrderResponse(existingOrder, isDuplicate);
    }

    private Map<Long, Integer> resolveItemsToCheckout(Long userId, CheckoutRequest request) {
        Map<Long, Integer> items = new LinkedHashMap<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Người dùng chỉ định danh sách variant cụ thể cần checkout
            Map<Object, Object> cartEntries = null;

            for (CheckoutItemRequest itemReq : request.getItems()) {
                if (itemReq == null || itemReq.getVariantId() == null) {
                    continue;
                }
                Long variantId = itemReq.getVariantId();
                Integer quantity = itemReq.getQuantity();

                if (quantity == null || quantity <= 0) {
                    // Nếu quantity null, tìm trong Redis Cart
                    if (cartEntries == null) {
                        cartEntries = stringRedisTemplate.opsForHash().entries(CART_KEY_PREFIX + userId);
                    }
                    Object cartVal = cartEntries.get(String.valueOf(variantId));
                    if (cartVal != null) {
                        try {
                            CartItemRedisDto dto = objectMapper.readValue(String.valueOf(cartVal), CartItemRedisDto.class);
                            quantity = dto.getQuantity();
                        } catch (Exception ex) {
                            log.warn("Không thể parse cart item {} từ Redis cho user {}", variantId, userId);
                        }
                    }
                }

                if (quantity == null || quantity <= 0) {
                    throw new BadRequestException(String.format("Không xác định được số lượng hợp lệ cho biến thể ID %d", variantId));
                }

                items.merge(variantId, quantity, Integer::sum);
            }
        } else {
            // Không truyền danh sách item -> Checkout toàn bộ giỏ hàng
            Map<Object, Object> cartEntries = stringRedisTemplate.opsForHash().entries(CART_KEY_PREFIX + userId);
            if (cartEntries == null || cartEntries.isEmpty()) {
                throw new BadRequestException("Giỏ hàng của bạn đang trống, không thể tiến hành đặt hàng.");
            }

            for (Map.Entry<Object, Object> entry : cartEntries.entrySet()) {
                try {
                    CartItemRedisDto dto = objectMapper.readValue(String.valueOf(entry.getValue()), CartItemRedisDto.class);
                    if (dto.getVariantId() != null && dto.getQuantity() != null && dto.getQuantity() > 0) {
                        items.put(dto.getVariantId(), dto.getQuantity());
                    }
                } catch (Exception ex) {
                    log.warn("Bỏ qua item lỗi {} khi đọc giỏ hàng của user {}", entry.getKey(), userId);
                }
            }
        }

        return items;
    }

    private List<ResolvedCheckoutItem> verifyAndSnapshotCatalogItems(Map<Long, Integer> itemsToCheckout) {
        List<ResolvedCheckoutItem> resolvedList = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : itemsToCheckout.entrySet()) {
            Long variantId = entry.getKey();
            int requestedQuantity = entry.getValue();

            // 2. Lấy lại giá, trạng thái sản phẩm và tồn kho từ Catalog Service (không tin Redis)
            Optional<CatalogVariantValidationDto> catalogOpt = catalogServiceClient.getVariantDetails(variantId);
            if (catalogOpt.isEmpty()) {
                throw new BadRequestException(String.format("Biến thể sản phẩm (ID: %d) không tồn tại trên hệ thống Catalog.", variantId));
            }

            CatalogVariantValidationDto catalogDto = catalogOpt.get();

            // 6. Xử lý trường hợp sản phẩm ngừng bán hoặc shop bị khóa
            if (!catalogDto.isActive()) {
                throw new BadRequestException(String.format("Sản phẩm '%s' hoặc gian hàng hiện không còn hoạt động kinh doanh.", catalogDto.getProductName()));
            }

            // 5. Không để số lượng vượt tồn kho khả dụng
            int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
            if (availableStock < requestedQuantity) {
                throw new BadRequestException(String.format(
                        "Sản phẩm '%s' không đủ tồn kho khả dụng (yêu cầu: %d, khả dụng: %d). Vui lòng cập nhật lại giỏ hàng.",
                        catalogDto.getProductName(), requestedQuantity, availableStock
                ));
            }

            resolvedList.add(ResolvedCheckoutItem.builder()
                    .variantId(catalogDto.getVariantId())
                    .productId(catalogDto.getProductId())
                    .shopId(catalogDto.getShopId())
                    .shopName(catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + catalogDto.getShopId())
                    .productName(catalogDto.getProductName())
                    .variantName(catalogDto.getVariantName())
                    .sku(catalogDto.getSku())
                    .imageUrl(catalogDto.getImageUrl())
                    .price(catalogDto.getPrice())
                    .quantity(requestedQuantity)
                    .build());
        }

        return resolvedList;
    }

    private MasterOrderResponse mapToMasterOrderResponse(MasterOrder masterOrder, boolean duplicateRequest) {
        List<SubOrderResponse> subOrderResponses = new ArrayList<>();

        if (masterOrder.getSubOrders() != null) {
            for (SubOrder subOrder : masterOrder.getSubOrders()) {
                List<OrderItemResponse> itemResponses = new ArrayList<>();
                if (subOrder.getItems() != null) {
                    for (OrderItem item : subOrder.getItems()) {
                        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        itemResponses.add(OrderItemResponse.builder()
                                .id(item.getId())
                                .variantId(item.getVariantId())
                                .productName(item.getProductName())
                                .variantName(item.getVariantName())
                                .imageUrl(item.getImageUrl())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(subtotal)
                                .build());
                    }
                }

                subOrderResponses.add(SubOrderResponse.builder()
                        .id(subOrder.getId())
                        .masterOrderId(masterOrder.getId())
                        .shopId(subOrder.getShopId())
                        .subtotalAmount(subOrder.getSubtotalAmount())
                        .shippingFee(subOrder.getShippingFee())
                        .shopVoucherDiscount(subOrder.getShopVoucherDiscount())
                        .shopVoucherCode(subOrder.getShopVoucherCode())
                        .finalAmount(subOrder.getFinalAmount())
                        .status(subOrder.getStatus())
                        .createdAt(subOrder.getCreatedAt())
                        .items(itemResponses)
                        .build());
            }
        }

        return MasterOrderResponse.builder()
                .id(masterOrder.getId())
                .userId(masterOrder.getUserId())
                .totalAmount(masterOrder.getTotalAmount())
                .voucherDiscount(masterOrder.getVoucherDiscount())
                .voucherCode(masterOrder.getVoucherCode())
                .shippingFeeTotal(masterOrder.getShippingFeeTotal())
                .status(masterOrder.getStatus())
                .paymentMethod(masterOrder.getPaymentMethod())
                .shippingAddress(masterOrder.getShippingAddress())
                .recipientName(masterOrder.getRecipientName())
                .recipientPhone(masterOrder.getRecipientPhone())
                .createdAt(masterOrder.getCreatedAt())
                .subOrders(subOrderResponses)
                .duplicateRequest(duplicateRequest)
                .build();
    }

    // DTO nội bộ lưu snapshot item đã xác minh từ Catalog
    @lombok.Data
    @lombok.Builder
    private static class ResolvedCheckoutItem {
        private Long variantId;
        private Long productId;
        private Long shopId;
        private String shopName;
        private String productName;
        private String variantName;
        private String sku;
        private String imageUrl;
        private BigDecimal price;
        private Integer quantity;
    }
}
