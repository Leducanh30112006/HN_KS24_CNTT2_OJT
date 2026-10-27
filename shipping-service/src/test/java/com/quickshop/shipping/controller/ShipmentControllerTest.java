package com.quickshop.shipping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import com.quickshop.shipping.exception.DuplicateShipmentException;
import com.quickshop.shipping.exception.InvalidShippingStatusException;
import com.quickshop.shipping.exception.InvalidWebhookSignatureException;
import com.quickshop.shipping.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ShipmentController.class, ShippingWebhookController.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ShipmentController & WebhookController Tests - REST API Vận chuyển & Webhook")
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    @DisplayName("POST /api/shipping/calculate-fee: Tính phí vận chuyển thành công => 200 OK")
    void testCalculateFee_Success() throws Exception {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hải Phòng")
                .weightGrams(500)
                .build();

        CalculateFeeResponse response = CalculateFeeResponse.builder()
                .shippingFee(BigDecimal.valueOf(30000))
                .chargeableWeightGrams(500)
                .distanceType("INTRA_REGION")
                .estimatedDeliveryDays("2-3 ngày")
                .carrierName("GHN")
                .isMock(true)
                .build();

        when(shipmentService.calculateShippingFee(any())).thenReturn(response);

        mockMvc.perform(post("/api/shipping/calculate-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.distanceType").value("INTRA_REGION"));
    }

    @Test
    @DisplayName("POST /api/shipping/shipments: Tạo vận đơn thành công => 201 Created")
    void testCreateShipment_Success() throws Exception {
        CreateShipmentRequest request = CreateShipmentRequest.builder()
                .subOrderId(101L)
                .shopId(1L)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .weightGrams(500)
                .build();

        ShipmentResponse response = ShipmentResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .shopId(1L)
                .trackingCode("MOCK_GHN_123456")
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(22000))
                .shippingStatus(ShippingStatus.READY_TO_PICK)
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .isMock(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(shipmentService.createShipment(any())).thenReturn(response);

        mockMvc.perform(post("/api/shipping/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subOrderId").value(101))
                .andExpect(jsonPath("$.data.trackingCode").value("MOCK_GHN_123456"))
                .andExpect(jsonPath("$.data.shippingStatus").value("READY_TO_PICK"));
    }

    @Test
    @DisplayName("POST /api/shipping/shipments: Trùng Sub-Order => 409 Conflict")
    void testCreateShipment_Duplicate_ThrowsConflict() throws Exception {
        CreateShipmentRequest request = CreateShipmentRequest.builder()
                .subOrderId(101L)
                .shopId(1L)
                .recipientName("Nguyễn Văn A")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .build();

        when(shipmentService.createShipment(any()))
                .thenThrow(new DuplicateShipmentException("Vận đơn cho đơn hàng Sub-Order #101 đã tồn tại"));

        mockMvc.perform(post("/api/shipping/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Vận đơn cho đơn hàng Sub-Order #101 đã tồn tại"));
    }

    @Test
    @DisplayName("GET /api/shipping/shipments/{subOrderId}: Lấy thông tin vận đơn => 200 OK")
    void testGetShipmentBySubOrderId_Success() throws Exception {
        ShipmentResponse response = ShipmentResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .trackingCode("MOCK_GHN_123456")
                .shippingStatus(ShippingStatus.DELIVERING)
                .shippingFee(BigDecimal.valueOf(22000))
                .build();

        when(shipmentService.getShipmentBySubOrderId(101L)).thenReturn(response);

        mockMvc.perform(get("/api/shipping/shipments/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingCode").value("MOCK_GHN_123456"))
                .andExpect(jsonPath("$.data.shippingStatus").value("DELIVERING"));
    }

    @Test
    @DisplayName("POST /api/shipping/webhooks/{carrier}: Tiếp nhận Webhook hợp lệ => 200 OK")
    void testHandleWebhook_Success() throws Exception {
        CarrierWebhookRequest request = CarrierWebhookRequest.builder()
                .eventId("EVT_12345")
                .carrierName("GHN")
                .trackingCode("MOCK_GHN_123456")
                .shippingStatus("DELIVERED")
                .description("Đã giao hàng thành công")
                .build();

        mockMvc.perform(post("/api/shipping/webhooks/ghn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Token", "ghn_secret_token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/shipping/webhooks/{carrier}: Sai chữ ký / Token webhook => 401 Unauthorized")
    void testHandleWebhook_InvalidSignature_Returns401() throws Exception {
        CarrierWebhookRequest request = CarrierWebhookRequest.builder()
                .eventId("EVT_12345")
                .carrierName("GHN")
                .trackingCode("MOCK_GHN_123456")
                .shippingStatus("DELIVERED")
                .build();

        doThrow(new InvalidWebhookSignatureException("Xác minh chữ ký / Token webhook thất bại"))
                .when(shipmentService).processWebhook(eq("ghn"), any(), any(), any());

        mockMvc.perform(post("/api/shipping/webhooks/ghn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Token", "invalid_token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Xác minh chữ ký / Token webhook thất bại"));
    }

    @Test
    @DisplayName("POST /api/shipping/webhooks/{carrier}: Sai chuyển đổi trạng thái => 400 Bad Request")
    void testHandleWebhook_InvalidTransition_Returns400() throws Exception {
        CarrierWebhookRequest request = CarrierWebhookRequest.builder()
                .eventId("EVT_12345")
                .carrierName("GHN")
                .trackingCode("MOCK_GHN_123456")
                .shippingStatus("DELIVERED")
                .build();

        doThrow(new InvalidShippingStatusException("Không thể chuyển trạng thái vận đơn"))
                .when(shipmentService).processWebhook(eq("ghn"), any(), any(), any());

        mockMvc.perform(post("/api/shipping/webhooks/ghn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không thể chuyển trạng thái vận đơn"));
    }
}
