package com.quickshop.order.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.CartItemResponse;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.CartShopGroupResponse;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.dto.checkout.*;
import com.quickshop.order.dto.voucher.VoucherValidationResponse;
import com.quickshop.order.entity.*;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.OrderItemRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.saga.OrderSagaOrchestrator;
import com.quickshop.order.service.CartService;
import com.quickshop.order.service.CheckoutService;
import com.quickshop.order.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final MasterOrderRepository masterOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final CatalogServiceClient catalogServiceClient;
    private final VoucherService voucherService;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    public static final BigDecimal DEFAULT_SHOP_SHIPPING_FEE = BigDecimal.valueOf(25000); // 25,000 VND / shop

    private record ItemSnapshot(
            Long variantId,
            Long productId,
            Long shopId,
            String shopName,
            String productName,
            String variantName,
            String sku,
            String imageUrl,
            BigDecimal price,
            Integer quantity,
            BigDecimal subtotal
    ) {}

    private record ShopOrderCalculation(
            Long shopId,
            String shopName,
            List<ItemSnapshot> items,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal voucherDiscount,
            BigDecimal finalAmount,
            String voucherCode
    ) {}

    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        validateCheckoutInput(userId, request);

        // 1. Thu thập và xác thực danh sách mặt hàng
        List<ItemSnapshot> validatedItems = collectAndValidateItems(userId, request);
        if (validatedItems.isEmpty()) {
            throw new BadRequestException("Không có mặt hàng hợp lệ nào để tiến hành đặt hàng");
        }

        // 2. Gom nhóm theo từng Shop (Multi-Shop Splitting)
        Map<Long, List<ItemSnapshot>> itemsByShop = validatedItems.stream()
                .collect(Collectors.groupingBy(ItemSnapshot::shopId, LinkedHashMap::new, Collectors.toList()));

        // 3. Tính toán chi phí từng Shop và áp dụng Voucher của từng Shop
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal shippingFeeTotal = BigDecimal.ZERO;
        BigDecimal totalShopVoucherDiscount = BigDecimal.ZERO;
        List<ShopOrderCalculation> shopCalculations = new ArrayList<>();

        for (Map.Entry<Long, List<ItemSnapshot>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ItemSnapshot> shopItems = entry.getValue();
            String shopName = shopItems.get(0).shopName();

            BigDecimal shopSubtotal = shopItems.stream()
                    .map(ItemSnapshot::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSubtotal = totalSubtotal.add(shopSubtotal);
            shippingFeeTotal = shippingFeeTotal.add(DEFAULT_SHOP_SHIPPING_FEE);

            // Kiểm tra và áp dụng Voucher riêng của Shop nếu người mua có chọn
            BigDecimal shopVoucherDiscount = BigDecimal.ZERO;
            String shopVoucherCode = (request.getShopVouchers() != null) ? request.getShopVouchers().get(shopId) : null;
            if (shopVoucherCode != null && !shopVoucherCode.isBlank()) {
                VoucherValidationResponse shopValidation = voucherService.validateAndCalculateVoucher(shopVoucherCode, shopId, shopSubtotal);
                if (shopValidation.isValid()) {
                    shopVoucherDiscount = shopValidation.getDiscountAmount();
                } else {
                    throw new BadRequestException(String.format("Voucher của Shop #%d [%s] không hợp lệ: %s",
                            shopId, shopVoucherCode, shopValidation.getMessage()));
                }
            }
            totalShopVoucherDiscount = totalShopVoucherDiscount.add(shopVoucherDiscount);

            BigDecimal shopFinalAmount = shopSubtotal.add(DEFAULT_SHOP_SHIPPING_FEE).subtract(shopVoucherDiscount);
            if (shopFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
                shopFinalAmount = BigDecimal.ZERO;
            }

            shopCalculations.add(new ShopOrderCalculation(
                    shopId,
                    shopName,
                    shopItems,
                    shopSubtotal,
                    DEFAULT_SHOP_SHIPPING_FEE,
                    shopVoucherDiscount,
                    shopFinalAmount,
                    shopVoucherCode
            ));
        }

        // 4. Áp dụng Voucher toàn sàn nếu có
        BigDecimal platformVoucherDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            VoucherValidationResponse platformValidation = voucherService.validateAndCalculateVoucher(
                    request.getVoucherCode(), null, totalSubtotal);
            if (platformValidation.isValid()) {
                platformVoucherDiscount = platformValidation.getDiscountAmount();
            } else {
                throw new BadRequestException(String.format("Voucher toàn sàn [%s] không hợp lệ: %s",
                        request.getVoucherCode(), platformValidation.getMessage()));
            }
        }

        // Tổng tiền MasterOrder = (Tổng tiền hàng + Tổng phí ship) - Tổng giảm shop - Giảm giá sàn
        BigDecimal masterTotalAmount = totalSubtotal.add(shippingFeeTotal)
                .subtract(totalShopVoucherDiscount)
                .subtract(platformVoucherDiscount);
        if (masterTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            masterTotalAmount = BigDecimal.ZERO;
        }

        // 5. Tạo MasterOrder
        String fullAddress = String.format("%s (%s) - %s",
                request.getRecipientName().trim(),
                request.getRecipientPhone().trim(),
                request.getShippingAddress().trim());

        MasterOrderStatus initialStatus = "COD".equalsIgnoreCase(request.getPaymentMethod())
                ? MasterOrderStatus.PROCESSING
                : MasterOrderStatus.PENDING_PAYMENT;

        MasterOrder masterOrder = MasterOrder.builder()
                .userId(userId)
                .totalAmount(masterTotalAmount)
                .voucherDiscount(platformVoucherDiscount)
                .shippingFeeTotal(shippingFeeTotal)
                .status(initialStatus)
                .paymentMethod(request.getPaymentMethod().trim().toUpperCase())
                .shippingAddress(fullAddress)
                .build();

        masterOrder = masterOrderRepository.save(masterOrder);
        Long masterOrderId = masterOrder.getId();

        // 6. Tạo SubOrders và OrderItems snapshot
        int totalItemsCount = 0;
        List<SubOrderResponse> subOrderResponses = new ArrayList<>();
        Map<Long, Long> subOrderIdsByShop = new HashMap<>();
        List<OrderItem> allSavedOrderItems = new ArrayList<>();

        for (ShopOrderCalculation calc : shopCalculations) {
            SubOrder subOrder = SubOrder.builder()
                    .masterOrderId(masterOrderId)
                    .shopId(calc.shopId())
                    .subtotalAmount(calc.subtotal())
                    .shippingFee(calc.shippingFee())
                    .shopVoucherDiscount(calc.voucherDiscount())
                    .finalAmount(calc.finalAmount())
                    .status(SubOrderStatus.CREATED)
                    .build();

            subOrder = subOrderRepository.save(subOrder);
            Long subOrderId = subOrder.getId();
            subOrderIdsByShop.put(calc.shopId(), subOrderId);

            List<OrderItemResponse> itemResponses = new ArrayList<>();
            for (ItemSnapshot item : calc.items()) {
                OrderItem orderItem = OrderItem.builder()
                        .subOrderId(subOrderId)
                        .variantId(item.variantId())
                        .productId(item.productId())
                        .productName(item.productName())
                        .variantName(item.variantName())
                        .sku(item.sku())
                        .imageUrl(item.imageUrl())
                        .price(item.price())
                        .quantity(item.quantity())
                        .build();

                orderItem = orderItemRepository.save(orderItem);
                allSavedOrderItems.add(orderItem);

                itemResponses.add(OrderItemResponse.builder()
                        .id(orderItem.getId())
                        .subOrderId(subOrderId)
                        .variantId(item.variantId())
                        .productId(item.productId())
                        .productName(item.productName())
                        .variantName(item.variantName())
                        .sku(item.sku())
                        .imageUrl(item.imageUrl())
                        .price(item.price())
                        .quantity(item.quantity())
                        .subtotal(item.subtotal())
                        .build());

                totalItemsCount += item.quantity();
            }

            subOrderResponses.add(SubOrderResponse.builder()
                    .id(subOrderId)
                    .subOrderCode("QS-SUB-" + (100000L + subOrderId))
                    .masterOrderId(masterOrderId)
                    .shopId(calc.shopId())
                    .shopName(calc.shopName())
                    .subtotalAmount(calc.subtotal())
                    .shippingFee(calc.shippingFee())
                    .shopVoucherDiscount(calc.voucherDiscount())
                    .finalAmount(calc.finalAmount())
                    .status(subOrder.getStatus())
                    .createdAt(subOrder.getCreatedAt() != null ? subOrder.getCreatedAt() : LocalDateTime.now())
                    .items(itemResponses)
                    .build());
        }

        // 7. Ghi nhận lịch sử sử dụng voucher (Voucher Sàn & Voucher Shop)
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank() && platformVoucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                voucherService.recordUsage(request.getVoucherCode(), userId, masterOrderId, null, platformVoucherDiscount);
            } catch (Exception ex) {
                log.warn("Lỗi khi ghi nhận sử dụng voucher sàn {}: {}", request.getVoucherCode(), ex.getMessage());
            }
        }

        for (ShopOrderCalculation calc : shopCalculations) {
            if (calc.voucherCode() != null && !calc.voucherCode().isBlank() && calc.voucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    Long subId = subOrderIdsByShop.get(calc.shopId());
                    voucherService.recordUsage(calc.voucherCode(), userId, masterOrderId, subId, calc.voucherDiscount());
                } catch (Exception ex) {
                    log.warn("Lỗi khi ghi nhận sử dụng voucher shop {}: {}", calc.voucherCode(), ex.getMessage());
                }
            }
        }

        // 8. Tự động dọn dẹp các mặt hàng đã checkout khỏi Redis Cart của người dùng
        List<Long> checkedOutVariantIds = validatedItems.stream().map(ItemSnapshot::variantId).toList();
        try {
            cartService.removeCartItems(userId, checkedOutVariantIds);
            log.info("Đã dọn dẹp {} mặt hàng đã thanh toán khỏi Redis Cart của user {}", checkedOutVariantIds.size(), userId);
        } catch (Exception ex) {
            log.warn("Không thể xóa mặt hàng checkout khỏi Redis Cart cho user {}: {}", userId, ex.getMessage());
        }

        // 9. Kích hoạt Saga Checkout Orchestrator (giữ tồn kho có TTL 15 phút qua OrderCreatedEvent)
        try {
            orderSagaOrchestrator.startCheckoutSaga(masterOrder, allSavedOrderItems);
        } catch (Exception ex) {
            log.error("Lỗi khi kích hoạt Saga Checkout Orchestrator cho MasterOrder #{}: {}", masterOrderId, ex.getMessage(), ex);
        }

        log.info("Tạo thành công MasterOrder #{} cho user {} với {} SubOrders, tổng tiền: {} VND (Giảm sàn: {} VND, Giảm Shop: {} VND)",
                masterOrderId, userId, subOrderResponses.size(), masterTotalAmount, platformVoucherDiscount, totalShopVoucherDiscount);

        return CheckoutResponse.builder()
                .masterOrderId(masterOrderId)
                .orderCode("QS-" + (100000L + masterOrderId))
                .userId(userId)
                .totalAmount(masterTotalAmount)
                .subtotalAmount(totalSubtotal)
                .shippingFeeTotal(shippingFeeTotal)
                .voucherDiscount(platformVoucherDiscount)
                .shopVoucherDiscountTotal(totalShopVoucherDiscount)
                .status(masterOrder.getStatus())
                .paymentMethod(masterOrder.getPaymentMethod())
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(fullAddress)
                .note(request.getNote())
                .totalShops(subOrderResponses.size())
                .totalItems(totalItemsCount)
                .createdAt(masterOrder.getCreatedAt() != null ? masterOrder.getCreatedAt() : LocalDateTime.now())
                .subOrders(subOrderResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse previewCheckout(Long userId, CheckoutRequest request) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId không hợp lệ");
        }

        List<ItemSnapshot> validatedItems = collectAndValidateItems(userId, request);
        if (validatedItems.isEmpty()) {
            throw new BadRequestException("Không có mặt hàng nào trong giỏ để xem trước thanh toán");
        }

        Map<Long, List<ItemSnapshot>> itemsByShop = validatedItems.stream()
                .collect(Collectors.groupingBy(ItemSnapshot::shopId, LinkedHashMap::new, Collectors.toList()));

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal shippingFeeTotal = BigDecimal.ZERO;
        BigDecimal totalShopVoucherDiscount = BigDecimal.ZERO;
        List<SubOrderResponse> subOrderResponses = new ArrayList<>();
        int totalItemsCount = 0;

        for (Map.Entry<Long, List<ItemSnapshot>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ItemSnapshot> shopItems = entry.getValue();
            String shopName = shopItems.get(0).shopName();

            BigDecimal shopSubtotal = shopItems.stream()
                    .map(ItemSnapshot::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSubtotal = totalSubtotal.add(shopSubtotal);
            shippingFeeTotal = shippingFeeTotal.add(DEFAULT_SHOP_SHIPPING_FEE);

            // Tính toán Voucher riêng của Shop cho preview
            BigDecimal shopVoucherDiscount = BigDecimal.ZERO;
            String shopVoucherCode = (request.getShopVouchers() != null) ? request.getShopVouchers().get(shopId) : null;
            if (shopVoucherCode != null && !shopVoucherCode.isBlank()) {
                VoucherValidationResponse shopValidation = voucherService.validateAndCalculateVoucher(shopVoucherCode, shopId, shopSubtotal);
                if (shopValidation.isValid()) {
                    shopVoucherDiscount = shopValidation.getDiscountAmount();
                }
            }
            totalShopVoucherDiscount = totalShopVoucherDiscount.add(shopVoucherDiscount);

            BigDecimal shopFinalAmount = shopSubtotal.add(DEFAULT_SHOP_SHIPPING_FEE).subtract(shopVoucherDiscount);
            if (shopFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
                shopFinalAmount = BigDecimal.ZERO;
            }

            List<OrderItemResponse> itemResponses = shopItems.stream().map(i -> OrderItemResponse.builder()
                    .variantId(i.variantId())
                    .productId(i.productId())
                    .productName(i.productName())
                    .variantName(i.variantName())
                    .sku(i.sku())
                    .imageUrl(i.imageUrl())
                    .price(i.price())
                    .quantity(i.quantity())
                    .subtotal(i.subtotal())
                    .build()).toList();

            subOrderResponses.add(SubOrderResponse.builder()
                    .shopId(shopId)
                    .shopName(shopName)
                    .subtotalAmount(shopSubtotal)
                    .shippingFee(DEFAULT_SHOP_SHIPPING_FEE)
                    .shopVoucherDiscount(shopVoucherDiscount)
                    .finalAmount(shopFinalAmount)
                    .status(SubOrderStatus.CREATED)
                    .items(itemResponses)
                    .build());

            totalItemsCount += shopItems.stream().mapToInt(ItemSnapshot::quantity).sum();
        }

        // Tính toán Voucher sàn cho preview
        BigDecimal platformVoucherDiscount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            VoucherValidationResponse platformValidation = voucherService.validateAndCalculateVoucher(
                    request.getVoucherCode(), null, totalSubtotal);
            if (platformValidation.isValid()) {
                platformVoucherDiscount = platformValidation.getDiscountAmount();
            }
        }

        BigDecimal masterTotalAmount = totalSubtotal.add(shippingFeeTotal)
                .subtract(totalShopVoucherDiscount)
                .subtract(platformVoucherDiscount);
        if (masterTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            masterTotalAmount = BigDecimal.ZERO;
        }

        return CheckoutResponse.builder()
                .userId(userId)
                .totalAmount(masterTotalAmount)
                .subtotalAmount(totalSubtotal)
                .shippingFeeTotal(shippingFeeTotal)
                .voucherDiscount(platformVoucherDiscount)
                .shopVoucherDiscountTotal(totalShopVoucherDiscount)
                .status(MasterOrderStatus.DRAFT)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "COD")
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .totalShops(subOrderResponses.size())
                .totalItems(totalItemsCount)
                .subOrders(subOrderResponses)
                .build();
    }

    private void validateCheckoutInput(Long userId, CheckoutRequest request) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId người mua không hợp lệ");
        }
        if (request == null) {
            throw new BadRequestException("Dữ liệu yêu cầu thanh toán không được để trống");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new BadRequestException("Phương thức thanh toán không được để trống");
        }
        if (request.getRecipientName() == null || request.getRecipientName().isBlank()) {
            throw new BadRequestException("Tên người nhận hàng không được để trống");
        }
        if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
            throw new BadRequestException("Số điện thoại nhận hàng không được để trống");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new BadRequestException("Địa chỉ giao hàng không được để trống");
        }
    }

    private List<ItemSnapshot> collectAndValidateItems(Long userId, CheckoutRequest request) {
        Map<Long, Integer> targetItems = new LinkedHashMap<>();

        // Nguồn 1: Danh sách items chỉ định trong request
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CheckoutItemDto item : request.getItems()) {
                if (item != null && item.getVariantId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    targetItems.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
                }
            }
        } else {
            // Nguồn 2: Tự động lấy toàn bộ từ Redis User Cart
            CartResponse userCart = cartService.getCart(userId);
            if (userCart != null && userCart.getShops() != null) {
                for (CartShopGroupResponse shop : userCart.getShops()) {
                    if (shop.getItems() != null) {
                        for (CartItemResponse item : shop.getItems()) {
                            if (item.getVariantId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                                targetItems.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
                            }
                        }
                    }
                }
            }
        }

        if (targetItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItemSnapshot> snapshots = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : targetItems.entrySet()) {
            Long variantId = entry.getKey();
            Integer requestedQty = entry.getValue();

            CatalogVariantValidationDto catalogDto = catalogServiceClient.getVariantDetails(variantId)
                    .orElseThrow(() -> new BadRequestException("Biến thể sản phẩm (variantId=" + variantId + ") không tồn tại hoặc đã ngừng kinh doanh"));

            if (!catalogDto.isActive()) {
                throw new BadRequestException(String.format("Sản phẩm '%s' (SKU: %s) hiện không hoạt động hoặc shop đã đóng cửa",
                        catalogDto.getProductName(), catalogDto.getSku()));
            }

            int availableStock = catalogDto.getAvailableStock() != null ? catalogDto.getAvailableStock() : 0;
            if (availableStock <= 0) {
                throw new BadRequestException(String.format("Sản phẩm '%s' (SKU: %s) hiện đã hết hàng trong kho",
                        catalogDto.getProductName(), catalogDto.getSku()));
            }

            if (requestedQty > availableStock) {
                throw new BadRequestException(String.format("Sản phẩm '%s' (SKU: %s) không đủ số lượng tồn kho khả dụng (còn: %d, yêu cầu: %d)",
                        catalogDto.getProductName(), catalogDto.getSku(), availableStock, requestedQty));
            }

            BigDecimal price = catalogDto.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(requestedQty));
            Long shopId = catalogDto.getShopId() != null ? catalogDto.getShopId() : 1L;
            String shopName = catalogDto.getShopName() != null ? catalogDto.getShopName() : "Shop #" + shopId;

            snapshots.add(new ItemSnapshot(
                    variantId,
                    catalogDto.getProductId(),
                    shopId,
                    shopName,
                    catalogDto.getProductName(),
                    catalogDto.getVariantName(),
                    catalogDto.getSku(),
                    catalogDto.getImageUrl(),
                    price,
                    requestedQty,
                    subtotal
            ));
        }

        return snapshots;
    }
}
