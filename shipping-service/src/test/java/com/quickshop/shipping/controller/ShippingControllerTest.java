package com.quickshop.shipping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.service.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShippingController.class)
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShippingService shippingService;

    @Test
    @DisplayName("API POST /api/shipping/calculate-fee trả về 200")
    void testCalculateFeeApi() throws Exception {
        CalculateFeeRequest req = CalculateFeeRequest.builder()
                .carrier("GHN")
                .fromProvince("Hà Nội")
                .toProvince("Đà Nẵng")
                .weightGrams(500)
                .build();

        CalculateFeeResponse res = CalculateFeeResponse.builder()
                .carrier("GHN")
                .shippingFee(BigDecimal.valueOf(35000))
                .estimatedDays(3)
                .build();

        when(shippingService.calculateFee(any(CalculateFeeRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/shipping/calculate-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shippingFee").value(35000));
    }

    @Test
    @DisplayName("API POST /api/shipping/create-shipment trả về 200")
    void testCreateShipmentApi() throws Exception {
        CreateShipmentRequest req = CreateShipmentRequest.builder()
                .subOrderId(1001L)
                .carrierName("GHN")
                .shippingFee(BigDecimal.valueOf(25000))
                .build();

        Shipment shipment = Shipment.builder()
                .id(1L)
                .subOrderId(1001L)
                .trackingCode("GHN-TRACK123")
                .carrierName("GHN")
                .shippingStatus("READY_TO_PICK")
                .build();

        when(shippingService.createShipment(any(CreateShipmentRequest.class))).thenReturn(shipment);

        mockMvc.perform(post("/api/shipping/create-shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingCode").value("GHN-TRACK123"));
    }

    @Test
    @DisplayName("API GET /api/shipping/track/{trackingCode} trả về 200")
    void testTrackShipmentApi() throws Exception {
        Shipment shipment = Shipment.builder()
                .id(1L)
                .trackingCode("GHN-TRACK123")
                .shippingStatus("DELIVERING")
                .build();

        when(shippingService.getShipmentByTrackingCode("GHN-TRACK123")).thenReturn(shipment);

        mockMvc.perform(get("/api/shipping/track/GHN-TRACK123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shippingStatus").value("DELIVERING"));
    }

    @Test
    @DisplayName("API GET /api/shipping trả về danh sách tất cả vận đơn 200")
    void testGetAllShipmentsApi() throws Exception {
        Shipment shipment = Shipment.builder()
                .id(1L)
                .trackingCode("GHN-TRACK123")
                .shippingStatus("DELIVERING")
                .build();

        when(shippingService.getAllShipments()).thenReturn(java.util.List.of(shipment));

        mockMvc.perform(get("/api/shipping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].trackingCode").value("GHN-TRACK123"));
    }
}
