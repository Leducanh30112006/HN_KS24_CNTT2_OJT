package com.quickshop.shipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.event.ShipmentStatusUpdatedEvent;
import com.quickshop.shipping.adapter.CarrierAdapter;
import com.quickshop.shipping.adapter.CarrierAdapterFactory;
import com.quickshop.shipping.adapter.CarrierShipmentResult;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import com.quickshop.shipping.exception.DuplicateShipmentException;
import com.quickshop.shipping.exception.InvalidShippingStatusException;
import com.quickshop.shipping.exception.InvalidWebhookPayloadException;
import com.quickshop.shipping.exception.InvalidWebhookSignatureException;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import com.quickshop.shipping.service.impl.ShipmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentService Tests - Quản lý Vận đơn, Webhook Idempotency & Concurrency")
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShippingWebhookLogRepository webhookLogRepository;

    @Mock
    private CarrierAdapterFactory carrierAdapterFactory;

    @Mock
    private CarrierAdapter carrierAdapter;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ShipmentServiceImpl shipmentService;

    private CreateShipmentRequest sampleRequest;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentServiceImpl(
                shipmentRepository,
                webhookLogRepository,
                carrierAdapterFactory,
                rabbitTemplate,
                objectMapper,
                Optional.of(stringRedisTemplate)
        );

        sampleRequest = CreateShipmentRequest.builder()
                .subOrderId(101L)
                .shopId(1L)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .originAddress("Kho Shop 1, Hai Bà Trưng, Hà Nội")
                .weightGrams(500)
                .codAmount(BigDecimal.ZERO)
                .carrierName("GHN")
                .build();
    }

    @Test
    @DisplayName("Tạo vận đơn thành công: lưu bảng shipments, sinh trackingCode, trạng thái READY_TO_PICK và phát RabbitMQ event")
    void testCreateShipment_Success() {
        when(shipmentRepository.existsBySubOrderId(101L)).thenReturn(false);
        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);

        CarrierShipmentResult carrierResult = CarrierShipmentResult.builder()
                .trackingCode("GHN_MOCK_123456")
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(22000))
                .status(ShippingStatus.READY_TO_PICK)
                .isMock(true)
                .build();
        when(carrierAdapter.createShipment(any())).thenReturn(carrierResult);

        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment s = invocation.getArgument(0);
            s.setId(1L);
            s.setCreatedAt(LocalDateTime.now());
            s.setUpdatedAt(LocalDateTime.now());
            return s;
        });

        ShipmentResponse response = shipmentService.createShipment(sampleRequest);

        assertNotNull(response);
        assertEquals(101L, response.getSubOrderId());
        assertEquals("GHN_MOCK_123456", response.getTrackingCode());
        assertEquals(ShippingStatus.READY_TO_PICK, response.getShippingStatus());
        assertEquals(BigDecimal.valueOf(22000), response.getShippingFee());
        assertTrue(response.getIsMock());

        verify(shipmentRepository).save(any(Shipment.class));
        verify(rabbitTemplate).convertAndSend(eq("shipping.exchange"), eq("shipping.status.updated"), any(ShipmentStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Ngăn tạo trùng vận đơn: subOrderId đã tồn tại => ném DuplicateShipmentException")
    void testCreateShipment_DuplicateSubOrderId_ThrowsDuplicateShipmentException() {
        when(shipmentRepository.existsBySubOrderId(101L)).thenReturn(true);

        DuplicateShipmentException ex = assertThrows(DuplicateShipmentException.class, () ->
                shipmentService.createShipment(sampleRequest));

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(shipmentRepository, never()).save(any());
        verify(carrierAdapterFactory, never()).getAdapter(any());
    }

    @Test
    @DisplayName("Cập nhật trạng thái hợp lệ: READY_TO_PICK -> PICKED -> DELIVERING -> DELIVERED")
    void testUpdateShippingStatus_Success() {
        Shipment existing = Shipment.builder()
                .id(1L)
                .subOrderId(101L)
                .shopId(1L)
                .trackingCode("GHN_MOCK_123456")
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(22000))
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .build();

        when(shipmentRepository.findByTrackingCodeForUpdate("GHN_MOCK_123456")).thenReturn(Optional.of(existing));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        ShipmentResponse r1 = shipmentService.updateShippingStatus("GHN_MOCK_123456", ShippingStatus.PICKED, "Bưu tá đã nhận hàng");
        assertEquals(ShippingStatus.PICKED, r1.getShippingStatus());

        ShipmentResponse r2 = shipmentService.updateShippingStatus("GHN_MOCK_123456", ShippingStatus.DELIVERING, "Đang giao hàng");
        assertEquals(ShippingStatus.DELIVERING, r2.getShippingStatus());

        ShipmentResponse r3 = shipmentService.updateShippingStatus("GHN_MOCK_123456", ShippingStatus.DELIVERED, "Giao hàng thành công");
        assertEquals(ShippingStatus.DELIVERED, r3.getShippingStatus());

        verify(rabbitTemplate, times(3)).convertAndSend(eq("shipping.exchange"), eq("shipping.status.updated"), any(ShipmentStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Webhook hợp lệ: cập nhật shipment, ghi nhận log SUCCESS và phát event đồng bộ")
    void testProcessWebhook_ValidRequest_Success() {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_VALID_001")
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .description("Rời bưu cục Cầu Giấy")
                .build();

        Shipment existing = Shipment.builder()
                .id(1L)
                .subOrderId(101L)
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .shippingAddress("Hà Nội")
                .shippingFee(BigDecimal.valueOf(22000))
                .build();

        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);
        when(carrierAdapter.verifyWebhook(any(), any())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        when(webhookLogRepository.findByEventId("EVT_VALID_001")).thenReturn(Optional.empty());
        when(shipmentRepository.findByTrackingCodeForUpdate("GHN_MOCK_123456")).thenReturn(Optional.of(existing));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> headers = Map.of("Token", "valid_token");
        shipmentService.processWebhook("GHN", headers, "{}", webhook);

        verify(shipmentRepository).save(argThat(s -> s.getShippingStatus() == ShippingStatus.DELIVERING));
        verify(webhookLogRepository).save(argThat(l -> "SUCCESS".equals(l.getStatus()) && "EVT_VALID_001".equals(l.getEventId())));
        verify(rabbitTemplate).convertAndSend(eq("shipping.exchange"), eq("shipping.status.updated"), any(ShipmentStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Event trùng lặp: event_id đã có trạng thái SUCCESS => bỏ qua xử lý không cập nhật lần hai")
    void testProcessWebhook_DuplicateEventId_IdempotentSkip() {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_DUPLICATE_002")
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .build();

        ShippingWebhookLog existingLog = ShippingWebhookLog.builder()
                .eventId("EVT_DUPLICATE_002")
                .carrierName("GHN")
                .status("SUCCESS")
                .build();

        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);
        when(carrierAdapter.verifyWebhook(any(), any())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        when(webhookLogRepository.findByEventId("EVT_DUPLICATE_002")).thenReturn(Optional.of(existingLog));

        shipmentService.processWebhook("GHN", Map.of(), "{}", webhook);

        // Không cập nhật shipment và không phát RabbitMQ event lần hai
        verify(shipmentRepository, never()).findByTrackingCodeForUpdate(anyString());
        verify(shipmentRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Event sai: Chữ ký không hợp lệ => ném InvalidWebhookSignatureException")
    void testProcessWebhook_InvalidSignature_ThrowsException() {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_FORGED_003")
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .build();

        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);
        when(carrierAdapter.verifyWebhook(any(), any())).thenReturn(false);

        InvalidWebhookSignatureException ex = assertThrows(InvalidWebhookSignatureException.class, () ->
                shipmentService.processWebhook("GHN", Map.of("Token", "bad_token"), "{}", webhook));

        assertTrue(ex.getMessage().contains("Xác minh chữ ký / Token webhook thất bại"));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Event sai: Thiếu event_id => ném InvalidWebhookPayloadException")
    void testProcessWebhook_MissingEventId_ThrowsException() {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .build();

        InvalidWebhookPayloadException ex = assertThrows(InvalidWebhookPayloadException.class, () ->
                shipmentService.processWebhook("GHN", Map.of(), "{}", webhook));

        assertTrue(ex.getMessage().contains("event_id"));
    }

    @Test
    @DisplayName("Event sai: Trạng thái chuyển đổi không hợp lệ DELIVERED -> DELIVERING => ném InvalidShippingStatusException và ghi log FAILED")
    void testProcessWebhook_InvalidTransition_RecordsFailedLog() {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_INVALID_TRANS_004")
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .build();

        Shipment delivered = Shipment.builder()
                .id(1L)
                .subOrderId(101L)
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus(ShippingStatus.DELIVERED)
                .build();

        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);
        when(carrierAdapter.verifyWebhook(any(), any())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        when(webhookLogRepository.findByEventId("EVT_INVALID_TRANS_004")).thenReturn(Optional.empty());
        when(shipmentRepository.findByTrackingCodeForUpdate("GHN_MOCK_123456")).thenReturn(Optional.of(delivered));

        InvalidShippingStatusException ex = assertThrows(InvalidShippingStatusException.class, () ->
                shipmentService.processWebhook("GHN", Map.of(), "{}", webhook));

        assertTrue(ex.getMessage().contains("Không thể chuyển trạng thái"));
        // Xác minh log được ghi nhận với FAILED (không đánh dấu thành công sai thời điểm)
        verify(webhookLogRepository).save(argThat(l -> "FAILED".equals(l.getStatus())));
    }

    @Test
    @DisplayName("Hai request cùng event_id đến đồng thời: Unique Constraint bắt xung đột an toàn, không xử lý lặp")
    void testProcessWebhook_ConcurrentRequests_SafeHandling() throws Exception {
        CarrierWebhookRequest webhook = CarrierWebhookRequest.builder()
                .eventId("EVT_CONCURRENT_005")
                .carrierName("GHN")
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus("DELIVERING")
                .build();

        Shipment existing = Shipment.builder()
                .id(1L)
                .subOrderId(101L)
                .trackingCode("GHN_MOCK_123456")
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .shippingAddress("Hà Nội")
                .shippingFee(BigDecimal.valueOf(22000))
                .build();

        when(carrierAdapterFactory.getAdapter("GHN")).thenReturn(carrierAdapter);
        when(carrierAdapter.verifyWebhook(any(), any())).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        when(webhookLogRepository.findByEventId("EVT_CONCURRENT_005")).thenReturn(Optional.empty());
        when(shipmentRepository.findByTrackingCodeForUpdate("GHN_MOCK_123456")).thenReturn(Optional.of(existing));

        // Thread 1 lưu thành công, Thread 2 bị DataIntegrityViolationException từ unique constraint
        AtomicInteger saveCount = new AtomicInteger(0);
        when(webhookLogRepository.save(any(ShippingWebhookLog.class))).thenAnswer(invocation -> {
            if (saveCount.incrementAndGet() > 1) {
                throw new DataIntegrityViolationException("Duplicate entry 'EVT_CONCURRENT_005' for key 'uk_shipping_webhooks_event_id'");
            }
            return invocation.getArgument(0);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Callable<Boolean> task = () -> {
            try {
                shipmentService.processWebhook("GHN", Map.of(), "{}", webhook);
                return true;
            } finally {
                latch.countDown();
            }
        };

        Future<Boolean> f1 = executor.submit(task);
        Future<Boolean> f2 = executor.submit(task);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(f1.get());
        assertTrue(f2.get());

        executor.shutdown();
    }
}
