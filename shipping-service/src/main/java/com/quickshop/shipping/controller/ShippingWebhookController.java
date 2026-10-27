package com.quickshop.shipping.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.common.dto.ApiResponse;
import com.quickshop.shipping.dto.CarrierWebhookRequest;
import com.quickshop.shipping.exception.InvalidWebhookPayloadException;
import com.quickshop.shipping.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipping/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping Webhook Controller", description = "Tiếp nhận Webhook Idempotency từ các đơn vị vận chuyển (GHN, GHTK, v.v.)")
public class ShippingWebhookController {

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{carrier}")
    @Operation(summary = "Tiếp nhận Webhook hành trình vận đơn từ hãng vận chuyển với xác thực nguồn gửi và Idempotency")
    public ResponseEntity<ApiResponse<Void>> handleCarrierWebhook(
            @PathVariable String carrier,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawPayload) {
        log.info("Received webhook HTTP POST from carrier: {}", carrier);

        CarrierWebhookRequest request;
        try {
            request = objectMapper.readValue(rawPayload, CarrierWebhookRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse carrier webhook JSON payload: {}", e.getMessage());
            throw new InvalidWebhookPayloadException("Định dạng JSON webhook không hợp lệ: " + e.getMessage());
        }

        shipmentService.processWebhook(carrier, headers, rawPayload, request);

        return ResponseEntity.ok(ApiResponse.ok("Xử lý webhook thành công", null));
    }
}
