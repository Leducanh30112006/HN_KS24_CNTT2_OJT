package com.quickshop.shipping.e2e;

import com.quickshop.common.event.ShipmentStatusUpdatedEvent;
import com.quickshop.shipping.config.TestShippingIntegrationConfig;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import com.quickshop.shipping.exception.DuplicateShipmentException;
import com.quickshop.shipping.exception.InvalidShippingStatusException;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import com.quickshop.shipping.service.ShipmentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestShippingIntegrationConfig.class)
@DisplayName("E2E Integration Tests - Shipping & Logistics Service (Flow 9)")
class ShippingServiceE2ETest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShippingWebhookLogRepository webhookLogRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private static final Long TEST_SUB_ORDER_ID = 9101L;
    private static final Long TEST_SHOP_ID = 10L;

    @BeforeEach
    void setUp() {
        TestShippingIntegrationConfig.clearAll();
        TestShippingIntegrationConfig.configureRedisMock(stringRedisTemplate);

        // Thu thập event phát ra qua RabbitTemplate
        org.mockito.Mockito.doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                TestShippingIntegrationConfig.PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class)
        );

        webhookLogRepository.deleteAll();
        shipmentRepository.deleteAll();
    }

    // =========================================================================
    // LUỒNG 9: SHIPPING WEBHOOK VÀ EVENT_ID TRÙNG (Tạo vận đơn, Webhook, Idempotency, Concurrency)
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("[Flow 9 - E2E] Tạo vận đơn: Lưu DB, sinh trackingCode, trạng thái READY_TO_PICK và chống tạo trùng")
    void testCreateShipment_Success_And_PreventDuplicate() {
        CreateShipmentRequest request = CreateShipmentRequest.builder()
                .subOrderId(TEST_SUB_ORDER_ID)
                .shopId(TEST_SHOP_ID)
                .recipientName("Trần Văn B")
                .recipientPhone("0912345678")
                .shippingAddress("Số 456 Hoàng Hoa Thám, Hà Nội")
                .originAddress("Kho Shop 10, Đống Đa, Hà Nội")
                .weightGrams(800)
                .codAmount(BigDecimal.ZERO)
                .carrierName("GHN")
                .build();

        // 1. Tạo vận đơn thành công
        ShipmentResponse response = shipmentService.createShipment(request);
        assertNotNull(response);
        assertNotNull(response.getTrackingCode());
        assertEquals(ShippingStatus.READY_TO_PICK, response.getShippingStatus());
        assertEquals("GHN", response.getCarrierName());

        // Kiểm tra Database
        Optional<Shipment> dbShipment = shipmentRepository.findBySubOrderId(TEST_SUB_ORDER_ID);
        assertTrue(dbShipment.isPresent(), "Shipment phải được lưu vào CSDL");
        assertEquals(ShippingStatus.READY_TO_PICK, dbShipment.get().getShippingStatus());

        // 2. Chống tạo trùng cho cùng Sub-Order
        assertThrows(DuplicateShipmentException.class, () ->
                shipmentService.createShipment(request)
        );
    }

    @Test
    @Transactional
    @DisplayName("[Flow 9 - E2E] Webhook Vận chuyển: Chuyển tiếp trạng thái PICKED -> DELIVERING -> DELIVERED, lưu log và phát event")
    void testCarrierWebhook_ValidTransitions_UpdatesDB_And_PublishesEvents() {
        // 1. Tạo shipment ban đầu ở trạng thái READY_TO_PICK
        Shipment shipment = shipmentRepository.save(Shipment.builder()
                .subOrderId(TEST_SUB_ORDER_ID)
                .shopId(TEST_SHOP_ID)
                .trackingCode("TRACK_E2E_001")
                .carrierName("GHN")
                .shippingFee(new BigDecimal("30000"))
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .recipientName("Trần Văn B")
                .recipientPhone("0912345678")
                .shippingAddress("Hà Nội")
                .originAddress("Hà Nội")
                .weightGrams(500)
                .build());

        // 2. Webhook 1: Đơn vị vận chuyển đã lấy hàng (PICKED)
        CarrierWebhookRequest webhook1 = CarrierWebhookRequest.builder()
                .eventId("EVT_PICKED_101")
                .carrierName("GHN")
                .trackingCode("TRACK_E2E_001")
                .shippingStatus("PICKED")
                .description("Shipper da lay hang tu nguoi ban")
                .build();

        shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", webhook1);

        Shipment shipmentPicked = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(ShippingStatus.PICKED, shipmentPicked.getShippingStatus(), "Trạng thái vận chuyển phải đổi sang PICKED");

        Optional<ShippingWebhookLog> log1 = webhookLogRepository.findByEventId("EVT_PICKED_101");
        assertTrue(log1.isPresent(), "Log webhook phải được lưu vào shipping_webhooks_log");
        assertEquals("SUCCESS", log1.get().getStatus());

        // 3. Webhook 2: Đang giao hàng (DELIVERING)
        CarrierWebhookRequest webhook2 = CarrierWebhookRequest.builder()
                .eventId("EVT_DELIVERING_102")
                .carrierName("GHN")
                .trackingCode("TRACK_E2E_001")
                .shippingStatus("DELIVERING")
                .description("Dang tren duong giao den khach hang")
                .build();

        shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", webhook2);

        Shipment shipmentDelivering = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(ShippingStatus.DELIVERING, shipmentDelivering.getShippingStatus());

        // 4. Webhook 3: Giao hàng thành công (DELIVERED)
        CarrierWebhookRequest webhook3 = CarrierWebhookRequest.builder()
                .eventId("EVT_DELIVERED_103")
                .carrierName("GHN")
                .trackingCode("TRACK_E2E_001")
                .shippingStatus("DELIVERED")
                .description("Giao hang thanh cong cho nguoi nhan")
                .build();

        shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", webhook3);

        Shipment shipmentDelivered = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assertEquals(ShippingStatus.DELIVERED, shipmentDelivered.getShippingStatus());
        assertNotNull(shipmentDelivered.getUpdatedAt());

        // Kiểm tra các sự kiện ShipmentStatusUpdatedEvent được bắn đi qua RabbitMQ
        assertFalse(TestShippingIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "RabbitMQ phải nhận được event cập nhật trạng thái");
        boolean hasDeliveredEvent = TestShippingIntegrationConfig.PUBLISHED_MESSAGES.stream()
                .anyMatch(msg -> msg instanceof ShipmentStatusUpdatedEvent &&
                        "DELIVERED".equals(((ShipmentStatusUpdatedEvent) msg).getShippingStatus()));
        assertTrue(hasDeliveredEvent, "Phải có event thông báo trạng thái DELIVERED cho Order & Escrow");
    }

    @Test
    @Transactional
    @DisplayName("[Flow 9 - E2E] Webhook event_id trùng lặp: Idempotent skip, không cập nhật lần hai và không phát duplicate event")
    void testCarrierWebhook_DuplicateEventId_IdempotentSkip() {
        // 1. Tạo shipment
        Shipment shipment = shipmentRepository.save(Shipment.builder()
                .subOrderId(TEST_SUB_ORDER_ID)
                .shopId(TEST_SHOP_ID)
                .trackingCode("TRACK_DUPLICATE_002")
                .carrierName("GHN")
                .shippingFee(new BigDecimal("30000"))
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .recipientName("Trần Văn B")
                .recipientPhone("0912345678")
                .shippingAddress("Hà Nội")
                .originAddress("Hà Nội")
                .weightGrams(500)
                .build());

        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_SAME_ID_999")
                .carrierName("GHN")
                .trackingCode("TRACK_DUPLICATE_002")
                .shippingStatus("PICKED")
                .description("Shipper da lay hang")
                .build();

        // 2. Gửi webhook lần 1 -> Thành công chuyển sang PICKED
        shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", webhook);
        assertEquals(1, TestShippingIntegrationConfig.PUBLISHED_MESSAGES.size());

        // 3. Gửi lại đúng webhook với cùng event_id lần 2 -> Idempotent skip
        TestShippingIntegrationConfig.PUBLISHED_MESSAGES.clear();
        shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", webhook);

        // Kiểm tra không phát thêm event mới lần 2
        assertTrue(TestShippingIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "Webhook trùng event_id không được phát thêm event mới");

        // Số lượng log trong DB chỉ có đúng 1 bản ghi cho event_id này
        long logCount = webhookLogRepository.findAll().stream()
                .filter(l -> "EVT_SAME_ID_999".equals(l.getEventId()))
                .count();
        assertEquals(1, logCount, "Chỉ duy nhất 1 bản ghi log cho event_id");
    }

    @Test
    @Transactional
    @DisplayName("[Flow 9 - E2E] Chuyển đổi trạng thái sai (DELIVERED -> DELIVERING): Từ chối và ghi log FAILED")
    void testCarrierWebhook_InvalidStatusTransition_Rejected() {
        Shipment deliveredShipment = shipmentRepository.save(Shipment.builder()
                .subOrderId(TEST_SUB_ORDER_ID)
                .shopId(TEST_SHOP_ID)
                .trackingCode("TRACK_DELIVERED_003")
                .carrierName("GHN")
                .shippingFee(new BigDecimal("30000"))
                .shippingStatus(ShippingStatus.DELIVERED) // Đã giao thành công
                .recipientName("Trần Văn B")
                .recipientPhone("0912345678")
                .shippingAddress("Hà Nội")
                .originAddress("Hà Nội")
                .weightGrams(500)
                .build());

        CarrierWebhookRequest invalidTransitionWebhook = CarrierWebhookRequest.builder()
                .eventId("EVT_INVALID_TRANS_888")
                .carrierName("GHN")
                .trackingCode("TRACK_DELIVERED_003")
                .shippingStatus("DELIVERING") // Cố gắng chuyển ngược về DELIVERING
                .description("Loi cap nhat trang thai cu")
                .build();

        assertThrows(InvalidShippingStatusException.class, () ->
                shipmentService.processWebhook("GHN", Map.of("Token", "test-ghn-token"), "{}", invalidTransitionWebhook)
        );

        // Trạng thái vận đơn trong DB phải giữ nguyên DELIVERED
        Shipment checked = shipmentRepository.findById(deliveredShipment.getId()).orElseThrow();
        assertEquals(ShippingStatus.DELIVERED, checked.getShippingStatus(), "Đơn đã DELIVERED không thể bị chuyển lùi trạng thái");

        // Kiểm tra log ghi nhận FAILED
        Optional<ShippingWebhookLog> failedLog = webhookLogRepository.findByEventId("EVT_INVALID_TRANS_888");
        assertTrue(failedLog.isPresent());
        assertEquals("FAILED", failedLog.get().getStatus());
    }
}
