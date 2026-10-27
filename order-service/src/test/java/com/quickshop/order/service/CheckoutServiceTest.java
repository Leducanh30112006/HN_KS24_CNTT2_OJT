package com.quickshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.client.CatalogServiceClient;
import com.quickshop.order.dto.cart.CartItemRedisDto;
import com.quickshop.order.dto.catalog.CatalogVariantValidationDto;
import com.quickshop.order.dto.checkout.CheckoutItemRequest;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;
import com.quickshop.order.dto.order.SubOrderResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.PaymentMethod;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.exception.CartLockException;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.saga.OrderSagaCoordinator;
import com.quickshop.order.service.impl.CheckoutServiceImpl;
import com.quickshop.order.service.impl.VoucherServiceImpl;
import com.quickshop.order.util.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private MasterOrderRepository masterOrderRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private CatalogServiceClient catalogServiceClient;

    @Mock
    private CartService cartService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @Mock
    private OrderSagaCoordinator orderSagaCoordinator;

    private ObjectMapper objectMapper;
    private CheckoutServiceImpl checkoutService;

    private static final Long USER_ID = 1001L;
    private static final Long SHOP_ID_1 = 10L;
    private static final Long SHOP_ID_2 = 20L;
    private static final Long VARIANT_ID_1 = 201L;
    private static final Long VARIANT_ID_2 = 202L;
    private static final Long VARIANT_ID_3 = 203L;
    private static final String LOCK_TOKEN = "test-checkout-lock-token";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        VoucherServiceImpl voucherService = new VoucherServiceImpl(voucherRepository);

        checkoutService = new CheckoutServiceImpl(
                masterOrderRepository,
                voucherRepository,
                voucherService,
                catalogServiceClient,
                cartService,
                stringRedisTemplate,
                objectMapper,
                redisDistributedLock,
                orderSagaCoordinator
        );
    }

    @Test
    @DisplayName("1. Checkout đơn hàng thành công với 1 shop: Tính toán đúng Subtotal, Phí ship, Lưu DB và Xóa item khỏi Redis Cart")
    void testCheckout_SingleShop_Success() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        CheckoutRequest request = CheckoutRequest.builder()
                .requestId("req-single-shop-123")
                .shippingAddress("123 Lê Duẩn, Hoàn Kiếm, Hà Nội")
                .recipientName("Nguyễn Văn Kiên")
                .recipientPhone("0987654321")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(
                        CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(2).build(),
                        CheckoutItemRequest.builder().variantId(VARIANT_ID_2).quantity(1).build()
                ))
                .build();

        // Mock Catalog Service trả về thông tin cho 2 variant cùng thuộc SHOP_ID_1
        CatalogVariantValidationDto var1 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(SHOP_ID_1)
                .shopName("Official Tech Store")
                .productName("Bàn phím cơ không dây")
                .variantName("Switch Blue - Led RGB")
                .sku("KEY-BLUE-RGB")
                .imageUrl("https://minio.quickshop.vn/keyboard.png")
                .price(BigDecimal.valueOf(500000))
                .stock(20)
                .reservedStock(0)
                .availableStock(20)
                .active(true)
                .build();

        CatalogVariantValidationDto var2 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_2)
                .productId(502L)
                .shopId(SHOP_ID_1)
                .shopName("Official Tech Store")
                .productName("Chuột công thái học")
                .variantName("Màu Đen")
                .sku("MOUSE-ERG-BLK")
                .imageUrl("https://minio.quickshop.vn/mouse.png")
                .price(BigDecimal.valueOf(300000))
                .stock(15)
                .reservedStock(0)
                .availableStock(15)
                .active(true)
                .build();

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(var1));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_2)).thenReturn(Optional.of(var2));

        // Mock lưu vào DB
        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder order = invocation.getArgument(0);
            order.setId(100L);
            long subIdCounter = 1L;
            for (SubOrder sub : order.getSubOrders()) {
                sub.setId(subIdCounter++);
                long itemIdCounter = 1L;
                for (OrderItem item : sub.getItems()) {
                    item.setId(itemIdCounter++);
                }
            }
            return order;
        });

        // When
        MasterOrderResponse response = checkoutService.checkout(USER_ID, null, request);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(MasterOrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(PaymentMethod.COD, response.getPaymentMethod());
        assertEquals("123 Lê Duẩn, Hoàn Kiếm, Hà Nội", response.getShippingAddress());
        assertFalse(response.isDuplicateRequest());

        // Tiền hàng Shop 1: (500,000 * 2) + (300,000 * 1) = 1,300,000 VND
        // Phí ship: 30,000 VND -> Tổng thanh toán: 1,330,000 VND
        assertEquals(BigDecimal.valueOf(30000), response.getShippingFeeTotal());
        assertEquals(BigDecimal.valueOf(1330000), response.getTotalAmount());

        assertEquals(1, response.getSubOrders().size());
        SubOrderResponse subOrder = response.getSubOrders().get(0);
        assertEquals(SHOP_ID_1, subOrder.getShopId());
        assertEquals(BigDecimal.valueOf(1300000), subOrder.getSubtotalAmount());
        assertEquals(BigDecimal.valueOf(30000), subOrder.getShippingFee());
        assertEquals(BigDecimal.valueOf(1330000), subOrder.getFinalAmount());
        assertEquals(SubOrderStatus.CREATED, subOrder.getStatus());
        assertEquals(2, subOrder.getItems().size());

        // Xác minh chỉ xóa 2 variant đã checkout khỏi Redis Cart
        verify(cartService, times(1)).removeCartItems(eq(USER_ID), argThat(list ->
                list.contains(VARIANT_ID_1) && list.contains(VARIANT_ID_2) && list.size() == 2
        ));

        // Xác minh gọi điểm tích hợp Saga
        verify(orderSagaCoordinator, times(1)).initiateCheckoutSaga(any(MasterOrder.class));

        // Xác minh lưu Idempotency key vào Redis
        verify(valueOperations, times(1)).set(
                eq("checkout:idempotency:1001:req-single-shop-123"),
                eq("100"),
                any()
        );

        // Xác minh giải phóng Lock
        verify(redisDistributedLock, times(1)).releaseLock(eq("lock:checkout:1001"), eq(LOCK_TOKEN));
    }

    @Test
    @DisplayName("2. Checkout đơn hàng đa shop (Multi-shop): Tách đúng các Sub-Orders theo shopId, tính riêng phí ship và tiền hàng")
    void testCheckout_MultiShop_Success() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("456 Cầu Giấy, Hà Nội")
                .paymentMethod(PaymentMethod.VNPAY)
                .items(List.of(
                        CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(1).build(), // Shop 1
                        CheckoutItemRequest.builder().variantId(VARIANT_ID_3).quantity(2).build()  // Shop 2
                ))
                .build();

        CatalogVariantValidationDto var1 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(SHOP_ID_1)
                .shopName("Official Tech Store")
                .productName("Bàn phím cơ")
                .price(BigDecimal.valueOf(500000))
                .availableStock(10)
                .active(true)
                .build();

        CatalogVariantValidationDto var3 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_3)
                .productId(601L)
                .shopId(SHOP_ID_2)
                .shopName("Fashion Trendy Shop")
                .productName("Áo thun phong cách Oversize")
                .price(BigDecimal.valueOf(200000))
                .availableStock(50)
                .active(true)
                .build();

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(var1));
        when(catalogServiceClient.getVariantDetails(VARIANT_ID_3)).thenReturn(Optional.of(var3));

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder order = invocation.getArgument(0);
            order.setId(200L);
            return order;
        });

        // When
        MasterOrderResponse response = checkoutService.checkout(USER_ID, null, request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getSubOrders().size());

        // Phí ship 2 shop: 30,000 * 2 = 60,000 VND
        // Shop 1 tiền hàng: 500,000 VND + 30,000 ship = 530,000 VND
        // Shop 2 tiền hàng: 400,000 VND + 30,000 ship = 430,000 VND
        // Tổng Master: 530,000 + 430,000 = 960,000 VND
        assertEquals(BigDecimal.valueOf(60000), response.getShippingFeeTotal());
        assertEquals(BigDecimal.valueOf(960000), response.getTotalAmount());

        Set<Long> shopIds = new HashSet<>();
        for (SubOrderResponse sub : response.getSubOrders()) {
            shopIds.add(sub.getShopId());
            assertEquals(SubOrderStatus.CREATED, sub.getStatus());
        }
        assertTrue(shopIds.contains(SHOP_ID_1));
        assertTrue(shopIds.contains(SHOP_ID_2));

        verify(cartService, times(1)).removeCartItems(eq(USER_ID), argThat(list ->
                list.contains(VARIANT_ID_1) && list.contains(VARIANT_ID_3) && list.size() == 2
        ));
    }

    @Test
    @DisplayName("3. Giỏ hàng rỗng và không truyền item -> Ném lỗi BadRequestException")
    void testCheckout_EmptyCart_ThrowsBadRequestException() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);
        when(stringRedisTemplate.opsForHash().entries("cart:" + USER_ID)).thenReturn(Collections.emptyMap());

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(null) // Rỗng
                .build();

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                checkoutService.checkout(USER_ID, null, request)
        );

        assertTrue(ex.getMessage().contains("trống"));
        verify(masterOrderRepository, never()).save(any());
        verify(redisDistributedLock, times(1)).releaseLock(anyString(), anyString());
    }

    @Test
    @DisplayName("4. Sản phẩm không tồn tại trong Catalog Service -> Ném lỗi BadRequestException")
    void testCheckout_VariantNotFoundInCatalog_ThrowsBadRequestException() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(CheckoutItemRequest.builder().variantId(9999L).quantity(1).build()))
                .build();

        when(catalogServiceClient.getVariantDetails(9999L)).thenReturn(Optional.empty());

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                checkoutService.checkout(USER_ID, null, request)
        );

        assertTrue(ex.getMessage().contains("không tồn tại"));
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Sản phẩm hoặc Shop ngừng hoạt động kinh doanh (active = false) -> Ném lỗi BadRequestException")
    void testCheckout_ProductInactive_ThrowsBadRequestException() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(1).build()))
                .build();

        CatalogVariantValidationDto inactiveDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Sản phẩm cấm bán")
                .active(false)
                .availableStock(10)
                .build();

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(inactiveDto));

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                checkoutService.checkout(USER_ID, null, request)
        );

        assertTrue(ex.getMessage().contains("không còn hoạt động kinh doanh"));
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Số lượng checkout vượt tồn kho khả dụng -> Ném lỗi BadRequestException")
    void testCheckout_InsufficientStock_ThrowsBadRequestException() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(5).build())) // Đặt 5
                .build();

        CatalogVariantValidationDto outOfStockDto = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productName("Tai nghe Bluetooth")
                .active(true)
                .availableStock(2) // Chỉ còn 2
                .build();

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(outOfStockDto));

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                checkoutService.checkout(USER_ID, null, request)
        );

        assertTrue(ex.getMessage().contains("không đủ tồn kho khả dụng"));
        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Request checkout lặp (Idempotency Retry): Không tạo đơn mới, trả về Master Order đã lưu")
    void testCheckout_IdempotencyRetry_ReturnsExistingOrder() {
        // Given
        String requestId = "idemp-req-777";
        String idempotencyKey = "checkout:idempotency:1001:" + requestId;

        when(valueOperations.get(idempotencyKey)).thenReturn("300"); // Đã có đơn hàng ID 300

        MasterOrder existingOrder = MasterOrder.builder()
                .id(300L)
                .userId(USER_ID)
                .totalAmount(BigDecimal.valueOf(800000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("Hà Nội")
                .subOrders(new ArrayList<>())
                .build();

        when(masterOrderRepository.findById(300L)).thenReturn(Optional.of(existingOrder));

        CheckoutRequest request = CheckoutRequest.builder()
                .requestId(requestId)
                .shippingAddress("Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .build();

        // When
        MasterOrderResponse response = checkoutService.checkout(USER_ID, null, request);

        // Then
        assertNotNull(response);
        assertEquals(300L, response.getId());
        assertTrue(response.isDuplicateRequest());

        // Không bao giờ gọi save vào DB hoặc xóa cart một lần nữa
        verify(masterOrderRepository, never()).save(any());
        verify(cartService, never()).removeCartItems(any(), any());
        verify(redisDistributedLock, never()).acquireLock(any(), anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("8. Áp dụng Voucher kép (Voucher riêng của Shop + Voucher toàn sàn) tính toán chính xác chiết khấu")
    void testCheckout_ShopAndPlatformVouchers_AppliedCorrectly() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(LOCK_TOKEN);

        String shopVoucherCode = "SHOPVIP10";
        String platformVoucherCode = "QUICKSHOP50";

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Đà Nẵng")
                .paymentMethod(PaymentMethod.WALLET)
                .voucherCode(platformVoucherCode)
                .shopVoucherCodes(Map.of(SHOP_ID_1, shopVoucherCode))
                .items(List.of(CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(2).build()))
                .build();

        // Hàng: 2 cái * 500,000 = 1,000,000 VND
        CatalogVariantValidationDto var1 = CatalogVariantValidationDto.builder()
                .variantId(VARIANT_ID_1)
                .productId(501L)
                .shopId(SHOP_ID_1)
                .productName("Loa Bluetooth")
                .price(BigDecimal.valueOf(500000))
                .availableStock(10)
                .active(true)
                .build();

        when(catalogServiceClient.getVariantDetails(VARIANT_ID_1)).thenReturn(Optional.of(var1));

        // Mock Shop Voucher: giảm 10% tối đa 100,000 VND (10% của 1,000,000 = 100,000 VND)
        Voucher shopVoucher = Voucher.builder()
                .id(1L)
                .code(shopVoucherCode)
                .shopId(SHOP_ID_1)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .maxDiscountAmount(BigDecimal.valueOf(100000))
                .minOrderValue(BigDecimal.valueOf(200000))
                .usageLimit(100)
                .usedCount(5)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(VoucherStatus.ACTIVE)
                .build();

        // Mock Platform Voucher: Giảm cố định 50,000 VND cho đơn từ 500,000 VND
        Voucher platformVoucher = Voucher.builder()
                .id(2L)
                .code(platformVoucherCode)
                .shopId(null) // Toàn sàn
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .minOrderValue(BigDecimal.valueOf(500000))
                .usageLimit(100)
                .usedCount(10)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus(shopVoucherCode, VoucherStatus.ACTIVE)).thenReturn(Optional.of(shopVoucher));
        when(voucherRepository.findByCodeAndStatus(platformVoucherCode, VoucherStatus.ACTIVE)).thenReturn(Optional.of(platformVoucher));
        when(voucherRepository.incrementUsageCountAtomic(eq(1L), any())).thenReturn(1);
        when(voucherRepository.incrementUsageCountAtomic(eq(2L), any())).thenReturn(1);

        when(masterOrderRepository.save(any(MasterOrder.class))).thenAnswer(invocation -> {
            MasterOrder order = invocation.getArgument(0);
            order.setId(500L);
            return order;
        });

        // When
        MasterOrderResponse response = checkoutService.checkout(USER_ID, null, request);

        // Then
        // Tiền hàng: 1,000,000 VND
        // Giảm voucher shop: 100,000 VND
        // Phí ship shop: 30,000 VND
        // SubOrder final = 1,000,000 + 30,000 - 100,000 = 930,000 VND
        // Giảm voucher sàn: 50,000 VND
        // Master total = 930,000 - 50,000 = 880,000 VND
        assertEquals(BigDecimal.valueOf(50000), response.getVoucherDiscount());
        assertEquals("QUICKSHOP50", response.getVoucherCode());
        assertEquals(BigDecimal.valueOf(880000), response.getTotalAmount());

        assertEquals(1, response.getSubOrders().size());
        SubOrderResponse subOrder = response.getSubOrders().get(0);
        assertEquals(BigDecimal.valueOf(100000), subOrder.getShopVoucherDiscount());
        assertEquals("SHOPVIP10", subOrder.getShopVoucherCode());
        assertEquals(BigDecimal.valueOf(930000), subOrder.getFinalAmount());

        // Kiểm tra gọi atomic update cho cả 2 voucher
        verify(voucherRepository, times(1)).incrementUsageCountAtomic(eq(1L), eq(VoucherStatus.ACTIVE));
        verify(voucherRepository, times(1)).incrementUsageCountAtomic(eq(2L), eq(VoucherStatus.ACTIVE));
    }

    @Test
    @DisplayName("9. Xung đột Concurrent Lock khi checkout -> Ném lỗi CartLockException")
    void testCheckout_LockContention_ThrowsCartLockException() {
        // Given
        when(redisDistributedLock.acquireLock(anyString(), anyLong(), anyInt(), anyLong())).thenReturn(null);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("Hồ Chí Minh")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(CheckoutItemRequest.builder().variantId(VARIANT_ID_1).quantity(1).build()))
                .build();

        // When & Then
        assertThrows(CartLockException.class, () ->
                checkoutService.checkout(USER_ID, null, request)
        );

        verify(masterOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Tra cứu chi tiết đơn hàng theo ID (getOrderById)")
    void testGetOrderById_Success() {
        // Given
        MasterOrder order = MasterOrder.builder()
                .id(999L)
                .userId(USER_ID)
                .totalAmount(BigDecimal.valueOf(500000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.COD)
                .shippingAddress("TP. Hồ Chí Minh")
                .subOrders(new ArrayList<>())
                .build();

        when(masterOrderRepository.findById(999L)).thenReturn(Optional.of(order));

        // When
        MasterOrderResponse response = checkoutService.getOrderById(USER_ID, 999L);

        // Then
        assertNotNull(response);
        assertEquals(999L, response.getId());
        assertEquals(USER_ID, response.getUserId());
    }

    @Test
    @DisplayName("11. Tra cứu chi tiết đơn hàng của người dùng khác -> Ném lỗi BadRequestException")
    void testGetOrderById_ForbiddenForOtherUser() {
        // Given
        MasterOrder order = MasterOrder.builder()
                .id(999L)
                .userId(9999L) // User khác sở hữu
                .build();

        when(masterOrderRepository.findById(999L)).thenReturn(Optional.of(order));

        // When & Then
        assertThrows(BadRequestException.class, () ->
                checkoutService.getOrderById(USER_ID, 999L)
        );
    }
}
