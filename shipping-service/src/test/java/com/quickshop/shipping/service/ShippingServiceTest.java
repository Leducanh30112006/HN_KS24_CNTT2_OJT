package com.quickshop.shipping.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.shipping.controller.ShippingController;
import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.dto.ShipmentStatusUpdateRequest;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import com.quickshop.shipping.service.impl.ShippingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShippingWebhookLogRepository webhookLogRepository;

    @InjectMocks
    private ShippingServiceImpl shippingService;

    private Shipment sampleShipment;

    @BeforeEach
    void setUp() {
        sampleShipment = Shipment.builder()
                .id(1L)
                .subOrderId(101L)
                .trackingCode("GHN-ABC12345")
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(25000))
                .shippingStatus("READY_TO_PICK")
                .weightGrams(500)
                .build();
    }

    @Test
    @DisplayName("Tính phí vận chuyển GHN tiêu chuẩn nội tỉnh")
    void testCalculateFee_GHN_Standard_IntraProvince() {
        CalculateFeeRequest req = CalculateFeeRequest.builder()
                .carrier("GHN")
                .fromProvince("Hà Nội")
                .toProvince("Hà Nội")
                .weightGrams(500)
                .isExpress(false)
                .build();

        CalculateFeeResponse res = shippingService.calculateFee(req);

        assertNotNull(res);
        assertEquals("GHN", res.getCarrier());
        assertEquals(BigDecimal.valueOf(25000), res.getShippingFee());
        assertEquals(3, res.getEstimatedDays());
    }

    @Test
    @DisplayName("Tính phí vận chuyển GHTK liên tỉnh có phụ thu cân nặng và hỏa tốc")
    void testCalculateFee_GHTK_InterProvince_Heavy_Express() {
        CalculateFeeRequest req = CalculateFeeRequest.builder()
                .carrier("GHTK")
                .fromProvince("Hà Nội")
                .toProvince("Hồ Chí Minh")
                .weightGrams(2000) // 1000g base + 2x500g extra = +10,000
                .isExpress(true) // +15,000
                .build();

        CalculateFeeResponse res = shippingService.calculateFee(req);

        // GHTK base: 22000 + 10000 (inter) + 10000 (extra 1kg) + 15000 (express) = 57,000
        assertNotNull(res);
        assertEquals("GHTK", res.getCarrier());
        assertEquals(BigDecimal.valueOf(57000), res.getShippingFee());
        assertEquals(2, res.getEstimatedDays());
    }

    @Test
    @DisplayName("Tạo vận đơn mới thành công với mã tracking định dạng chuẩn")
    void testCreateShipment_Success() {
        CreateShipmentRequest req = CreateShipmentRequest.builder()
                .subOrderId(200L)
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(30000))
                .recipientName("Nguyen Van A")
                .recipientPhone("0987654321")
                .weightGrams(800)
                .build();

        when(shipmentRepository.findBySubOrderId(200L)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment created = shippingService.createShipment(req);

        assertNotNull(created);
        assertEquals(200L, created.getSubOrderId());
        assertEquals("GHN", created.getCarrierName());
        assertTrue(created.getTrackingCode().startsWith("GHN-"));
        assertEquals("READY_TO_PICK", created.getShippingStatus());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Tạo vận đơn ném lỗi khi thiếu subOrderId")
    void testCreateShipment_MissingSubOrderId() {
        CreateShipmentRequest req = CreateShipmentRequest.builder().build();
        assertThrows(BadRequestException.class, () -> shippingService.createShipment(req));
    }

    @Test
    @DisplayName("Xử lý webhook vận chuyển thành công cho sự kiện mới")
    void testHandleWebhook_NewEvent_Success() {
        ShippingController.WebhookPayload payload = new ShippingController.WebhookPayload();
        payload.setEventId("EVT-1001");
        payload.setTrackingCode("GHN-ABC12345");
        payload.setCarrierName("GHN");
        payload.setStatus("DELIVERING");

        when(webhookLogRepository.existsByEventId("EVT-1001")).thenReturn(false);
        when(shipmentRepository.findByTrackingCode("GHN-ABC12345")).thenReturn(Optional.of(sampleShipment));

        boolean processed = shippingService.handleWebhook(payload);

        assertTrue(processed);
        verify(webhookLogRepository, times(1)).save(any(ShippingWebhookLog.class));
        verify(shipmentRepository, times(1)).save(sampleShipment);
        assertEquals("DELIVERING", sampleShipment.getShippingStatus());
    }

    @Test
    @DisplayName("Xử lý webhook Idempotent: Bỏ qua sự kiện trùng event_id")
    void testHandleWebhook_DuplicateEvent_Idempotent() {
        ShippingController.WebhookPayload payload = new ShippingController.WebhookPayload();
        payload.setEventId("EVT-1001");
        payload.setTrackingCode("GHN-ABC12345");

        when(webhookLogRepository.existsByEventId("EVT-1001")).thenReturn(true);

        boolean processed = shippingService.handleWebhook(payload);

        assertFalse(processed);
        verify(webhookLogRepository, never()).save(any(ShippingWebhookLog.class));
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    @DisplayName("Mô phỏng bước vận chuyển kế tiếp (READY_TO_PICK -> PICKED)")
    void testSimulateNextCarrierStep() {
        when(shipmentRepository.findByTrackingCode("GHN-ABC12345")).thenReturn(Optional.of(sampleShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment next = shippingService.simulateNextCarrierStep("GHN-ABC12345");

        assertEquals("PICKED", next.getShippingStatus());
    }
}
