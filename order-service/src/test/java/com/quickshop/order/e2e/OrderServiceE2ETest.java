package com.quickshop.order.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.event.RollbackEvent;
import com.quickshop.common.event.StockReservedEvent;
import com.quickshop.order.config.TestIntegrationConfig;
import com.quickshop.order.dto.cart.AddToCartRequest;
import com.quickshop.order.dto.cart.CartResponse;
import com.quickshop.order.dto.cart.GuestCartItemDto;
import com.quickshop.order.dto.cart.MergeCartRequest;
import com.quickshop.order.dto.cart.MergeCartResponse;
import com.quickshop.order.dto.checkout.CheckoutItemRequest;
import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.order.MasterOrderResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.OrderItem;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.PaymentMethod;
import com.quickshop.order.entity.enums.SubOrderStatus;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.saga.OrderSagaCoordinator;
import com.quickshop.order.scheduler.OrderTimeoutScheduler;
import com.quickshop.order.service.CartService;
import com.quickshop.order.service.CheckoutService;
import com.quickshop.order.service.VoucherService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestIntegrationConfig.class)
@DisplayName("E2E Integration Tests - Order & Cart Service (Flows: 1, 2, 3, 4, 5, 11)")
class OrderServiceE2ETest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private MasterOrderRepository masterOrderRepository;

    @Autowired
    private SubOrderRepository subOrderRepository;

    @Autowired
    private OrderSagaCoordinator orderSagaCoordinator;

    @Autowired
    private OrderTimeoutScheduler orderTimeoutScheduler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private net.javacrumbs.shedlock.core.LockProvider lockProvider;

    private static final Long TEST_USER_ID = 8888L;

    @BeforeEach
    void setUp() {
        TestIntegrationConfig.clearAll();
        TestIntegrationConfig.configureRedisMock(stringRedisTemplate);

        net.javacrumbs.shedlock.core.SimpleLock simpleLock = org.mockito.Mockito.mock(net.javacrumbs.shedlock.core.SimpleLock.class);
        org.mockito.Mockito.when(lockProvider.lock(org.mockito.ArgumentMatchers.any(net.javacrumbs.shedlock.core.LockConfiguration.class)))
                .thenReturn(Optional.of(simpleLock));

        subOrderRepository.deleteAll();
        masterOrderRepository.deleteAll();
        voucherRepository.deleteAll();

        // Cấu hình mock RabbitTemplate để thu thập message phát đi
        org.mockito.Mockito.doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                TestIntegrationConfig.PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class),
                org.mockito.ArgumentMatchers.any(org.springframework.amqp.core.MessagePostProcessor.class)
        );

        org.mockito.Mockito.doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                TestIntegrationConfig.PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class)
        );
    }

    // =========================================================================
    // LUỒNG 1: GUEST CART MERGE (Hợp nhất giỏ hàng & Chống lặp)
    // =========================================================================
    @Test
    @DisplayName("[Flow 1 - E2E] Guest Cart Merge: Hợp nhất biến thể, cộng dồn số lượng và kiểm tra Idempotency")
    void testGuestCartMerge_Success_And_Idempotency() {
        // 1. Chuẩn bị: User Cart ban đầu đã có variant 1 với số lượng 2
        cartService.addToCart(TEST_USER_ID, AddToCartRequest.builder()
                .variantId(1L)
                .quantity(2)
                .build());

        // 2. Frontend gửi request merge sau khi đăng nhập: variant 1 (3 cái) và variant 2 (5 cái)
        String requestId = "req-merge-" + UUID.randomUUID();
        MergeCartRequest mergeRequest = MergeCartRequest.builder()
                .requestId(requestId)
                .items(List.of(
                        GuestCartItemDto.builder().variantId(1L).quantity(3).build(),
                        GuestCartItemDto.builder().variantId(2L).quantity(5).build()
                ))
                .build();

        // Thực thi merge lần 1
        MergeCartResponse mergeResponse = cartService.mergeCart(TEST_USER_ID, mergeRequest);

        assertNotNull(mergeResponse);
        assertNotNull(mergeResponse.getCart());
        CartResponse mergedCart = mergeResponse.getCart();
        // Variant 1: 2 ban đầu + 3 merge = 5
        // Variant 2: 5 merge
        int totalQty = mergedCart.getShops().stream()
                .flatMap(s -> s.getItems().stream())
                .mapToInt(com.quickshop.order.dto.cart.CartItemResponse::getQuantity)
                .sum();
        assertEquals(10, totalQty, "Tổng số lượng trong giỏ sau merge phải là 2 + 3 + 5 = 10");

        // 3. Kiểm thử Retry an toàn (Idempotency): Gửi lại đúng request với cùng requestId
        MergeCartResponse retryResponse = cartService.mergeCart(TEST_USER_ID, mergeRequest);
        assertTrue(retryResponse.isDuplicateRequest(), "Retry request cùng requestId phải được đánh dấu duplicateRequest");
        CartResponse retryCart = retryResponse.getCart();
        int retryQty = retryCart.getShops().stream()
                .flatMap(s -> s.getItems().stream())
                .mapToInt(com.quickshop.order.dto.cart.CartItemResponse::getQuantity)
                .sum();
        assertEquals(10, retryQty, "Idempotent Retry không được làm tăng số lượng giỏ hàng lần 2");
    }

    // =========================================================================
    // LUỒNG 2: CHECKOUT NHIỀU SHOP (Multi-Shop Checkout & Tách Sub-Orders)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 2 - E2E] Checkout nhiều shop: Tạo 1 MasterOrder và tách các SubOrders lưu vào Database")
    void testMultiShopCheckout_CreatesMasterOrderAndSubOrders_DatabasePersisted() {
        // 1. Thêm sản phẩm từ Shop 1 (variantId 1) và Shop 2 (variantId 2) vào giỏ
        cartService.addToCart(TEST_USER_ID, AddToCartRequest.builder().variantId(1L).quantity(2).build());
        cartService.addToCart(TEST_USER_ID, AddToCartRequest.builder().variantId(2L).quantity(1).build());

        // 2. Gửi request Checkout
        String idempotencyKey = "idemp-checkout-" + UUID.randomUUID();
        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .requestId(idempotencyKey)
                .shippingAddress("Số 123 Đường Cầu Giấy, Hà Nội")
                .recipientName("Nguyễn Văn Kiên")
                .recipientPhone("0987654321")
                .paymentMethod(PaymentMethod.VNPAY)
                .items(List.of(
                        CheckoutItemRequest.builder().variantId(1L).quantity(2).build(),
                        CheckoutItemRequest.builder().variantId(2L).quantity(1).build()
                ))
                .build();

        MasterOrderResponse response = checkoutService.checkout(TEST_USER_ID, idempotencyKey, checkoutRequest);

        // 3. Kiểm tra kết quả API trả về
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(MasterOrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(2, response.getSubOrders().size(), "Phải tách thành 2 Sub-Orders cho 2 shop khác nhau");

        // 4. Kiểm tra TRỰC TIẾP TRẠNG THÁI DATABASE
        Optional<MasterOrder> dbMasterOrder = masterOrderRepository.findById(response.getId());
        assertTrue(dbMasterOrder.isPresent(), "MasterOrder phải được lưu thành công vào Database");
        MasterOrder savedMaster = dbMasterOrder.get();
        assertEquals(TEST_USER_ID, savedMaster.getUserId());
        assertEquals(MasterOrderStatus.PENDING_PAYMENT, savedMaster.getStatus());
        assertEquals(PaymentMethod.VNPAY, savedMaster.getPaymentMethod());

        List<SubOrder> dbSubOrders = subOrderRepository.findAll();
        assertEquals(2, dbSubOrders.size(), "Database phải chứa đúng 2 SubOrder");
        for (SubOrder subOrder : dbSubOrders) {
            assertEquals(savedMaster.getId(), subOrder.getMasterOrder().getId());
            assertFalse(subOrder.getItems().isEmpty(), "Mỗi Sub-Order phải có order_items đính kèm");
            assertEquals(SubOrderStatus.CREATED, subOrder.getStatus());
        }

        // 5. Kiểm tra tính Idempotent của Checkout
        MasterOrderResponse duplicateResponse = checkoutService.checkout(TEST_USER_ID, idempotencyKey, checkoutRequest);
        assertEquals(response.getId(), duplicateResponse.getId(), "Request checkout trùng idempotencyKey phải trả về đơn hàng cũ");
        assertEquals(2, subOrderRepository.findAll().size(), "Không được tạo thêm bản ghi SubOrder mới trong DB");
    }

    // =========================================================================
    // LUỒNG 3: VOUCHER SÀN VÀ VOUCHER SHOP (Dual Voucher & Hoàn tác)
    // =========================================================================
    @Test
    @DisplayName("[Flow 3 - E2E] Voucher sàn và voucher shop: Áp dụng voucher kép, ghi nhận giảm giá và hoàn tác khi hủy")
    void testDualVoucherApplication_And_RollbackOnCancellation() {
        // 1. Tạo Voucher sàn và Voucher shop trong Database
        Voucher platformVoucher = voucherRepository.save(Voucher.builder()
                .code("QUICKSHOP10")
                .shopId(null) // Voucher toàn sàn
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10)) // 10%
                .minOrderValue(BigDecimal.valueOf(100000))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(VoucherStatus.ACTIVE)
                .build());

        Voucher shopVoucher = voucherRepository.save(Voucher.builder()
                .code("SHOP10K")
                .shopId(1L) // Voucher của Shop 1
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(10000)) // 10k
                .minOrderValue(BigDecimal.valueOf(100000))
                .usageLimit(50)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(VoucherStatus.ACTIVE)
                .build());

        // 2. Thêm hàng vào giỏ và checkout áp dụng cả 2 voucher
        cartService.addToCart(TEST_USER_ID, AddToCartRequest.builder().variantId(1L).quantity(2).build());

        String idempotencyKey = "idemp-voucher-" + UUID.randomUUID();
        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .requestId(idempotencyKey)
                .shippingAddress("Hà Nội")
                .recipientName("Kiên")
                .recipientPhone("0987654321")
                .paymentMethod(PaymentMethod.VNPAY)
                .items(List.of(CheckoutItemRequest.builder().variantId(1L).quantity(2).build()))
                .voucherCode("QUICKSHOP10")
                .shopVoucherCodes(Map.of(1L, "SHOP10K"))
                .build();

        MasterOrderResponse response = checkoutService.checkout(TEST_USER_ID, idempotencyKey, checkoutRequest);

        // 3. Kiểm tra DB: Giảm giá đã được trừ và used_count tăng
        MasterOrder masterOrder = masterOrderRepository.findById(response.getId()).orElseThrow();
        assertTrue(masterOrder.getVoucherDiscount().compareTo(BigDecimal.ZERO) > 0, "MasterOrder phải ghi nhận voucher sàn");

        Voucher updatedPlatform = voucherRepository.findByCode("QUICKSHOP10").orElseThrow();
        assertEquals(1, updatedPlatform.getUsedCount(), "Voucher sàn used_count phải tăng lên 1");

        Voucher updatedShop = voucherRepository.findByCode("SHOP10K").orElseThrow();
        assertEquals(1, updatedShop.getUsedCount(), "Voucher shop used_count phải tăng lên 1");

        // 4. Kiểm thử hoàn tác Voucher khi hủy đơn
        voucherService.releaseVoucherUsage("QUICKSHOP10");
        voucherService.releaseVoucherUsage("SHOP10K");

        assertEquals(0, voucherRepository.findByCode("QUICKSHOP10").get().getUsedCount(), "Hoàn tác: voucher sàn quay về 0");
        assertEquals(0, voucherRepository.findByCode("SHOP10K").get().getUsedCount(), "Hoàn tác: voucher shop quay về 0");
    }

    // =========================================================================
    // LUỒNG 4 & 5: GIỮ KHO THÀNH CÔNG / THẤT BẠI & SAGA BÙ TRỪ / DLQ
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 4 & 5 - E2E] Saga: Giữ kho thất bại kích hoạt compensating action, hủy đơn và phát RollbackEvent")
    void testInventoryReservationFailure_TriggersCompensatingAction_AndRollbackEvent() {
        // 1. Tạo đơn hàng MasterOrder và SubOrder trong DB
        MasterOrder masterOrder = MasterOrder.builder()
                .userId(TEST_USER_ID)
                .totalAmount(BigDecimal.valueOf(250000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingAddress("Hà Nội")
                .subOrders(new ArrayList<>())
                .build();

        SubOrder subOrder = SubOrder.builder()
                .shopId(1L)
                .subtotalAmount(BigDecimal.valueOf(250000))
                .finalAmount(BigDecimal.valueOf(250000))
                .status(SubOrderStatus.CREATED)
                .build();
        masterOrder.addSubOrder(subOrder);
        final MasterOrder savedMaster = masterOrderRepository.save(masterOrder);

        entityManager.flush();
        entityManager.clear();

        // 2. Nhận sự kiện Giữ kho thất bại (Out of stock)
        StockReservedEvent failedEvent = StockReservedEvent.builder()
                .orderId(String.valueOf(savedMaster.getId()))
                .success(false)
                .reason("Hết hàng tồn kho biến thể SKU-VAR-1")
                .build();

        orderSagaCoordinator.handleStockReservedFailed(failedEvent, UUID.randomUUID().toString());

        // 3. Kiểm tra DB: MasterOrder và SubOrders chuyển sang CANCELLED
        MasterOrder cancelledOrder = masterOrderRepository.findById(savedMaster.getId()).orElseThrow();
        assertEquals(MasterOrderStatus.CANCELLED, cancelledOrder.getStatus(), "MasterOrder phải bị hủy do giữ kho thất bại");

        SubOrder cancelledSub = subOrderRepository.findById(subOrder.getId()).orElseThrow();
        assertEquals(SubOrderStatus.CANCELLED, cancelledSub.getStatus(), "SubOrder phải bị hủy tương ứng");

        // 4. Kiểm tra sự kiện Rollback được phát đi
        assertFalse(TestIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "Phải phát thông điệp bồi hoàn qua RabbitMQ");
        boolean hasRollbackEvent = TestIntegrationConfig.PUBLISHED_MESSAGES.stream()
                .anyMatch(msg -> msg instanceof RollbackEvent && ((RollbackEvent) msg).getOrderId().equals(String.valueOf(savedMaster.getId())));
        assertTrue(hasRollbackEvent, "Phải phát RollbackEvent cho Catalog Service");
    }

    // =========================================================================
    // LUỒNG 11: SCHEDULED TASK HỦY ĐƠN QUÁ HẠN (ShedLock & Bảo vệ đơn đã thanh toán)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 11 - E2E] Scheduled Task: Quét đơn PENDING_PAYMENT quá 15 phút để hủy và bảo vệ đơn đã thanh toán")
    void testOrderTimeoutScheduler_CancelsExpired_ProtectsPaidOrders() throws Exception {
        // 1. Tạo đơn 1: Quá hạn 20 phút, trạng thái PENDING_PAYMENT
        MasterOrder expiredOrder = masterOrderRepository.save(MasterOrder.builder()
                .userId(TEST_USER_ID)
                .totalAmount(BigDecimal.valueOf(300000))
                .status(MasterOrderStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingAddress("Hà Nội")
                .build());
        entityManager.createNativeQuery("UPDATE master_orders SET created_at = :ts WHERE id = :id")
                .setParameter("ts", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)))
                .setParameter("id", expiredOrder.getId())
                .executeUpdate();

        // 2. Tạo đơn 2: Quá hạn 20 phút nhưng đã thanh toán (PROCESSING)
        MasterOrder paidOrder = masterOrderRepository.save(MasterOrder.builder()
                .userId(TEST_USER_ID)
                .totalAmount(BigDecimal.valueOf(500000))
                .status(MasterOrderStatus.PROCESSING)
                .paymentMethod(PaymentMethod.VNPAY)
                .shippingAddress("Hà Nội")
                .build());
        entityManager.createNativeQuery("UPDATE master_orders SET created_at = :ts WHERE id = :id")
                .setParameter("ts", java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(20)))
                .setParameter("id", paidOrder.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // 3. Kích hoạt Scheduler quét hủy đơn quá hạn
        orderTimeoutScheduler.scanAndCancelExpiredOrders();

        // 4. Kiểm tra DB: Đơn quá hạn bị hủy, đơn đã thanh toán được bảo vệ an toàn
        MasterOrder checkedExpired = masterOrderRepository.findById(expiredOrder.getId()).orElseThrow();
        assertEquals(MasterOrderStatus.CANCELLED, checkedExpired.getStatus(), "Đơn PENDING_PAYMENT quá 15 phút phải bị hủy");

        MasterOrder checkedPaid = masterOrderRepository.findById(paidOrder.getId()).orElseThrow();
        assertEquals(MasterOrderStatus.PROCESSING, checkedPaid.getStatus(), "Đơn đã thanh toán (PROCESSING) tuyệt đối không bị hủy");
    }
}
