package com.quickshop.shipping.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.service.ShippingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<CalculateFeeResponse>> calculateFee(@RequestBody CalculateFeeRequest request) {
        CalculateFeeResponse response = shippingService.calculateFee(request);
        return ResponseEntity.ok(ApiResponse.success("Tính phí vận chuyển thành công", response));
    }

    @PostMapping("/create-shipment")
    public ResponseEntity<ApiResponse<Shipment>> createShipment(@RequestBody CreateShipmentRequest request) {
        Shipment shipment = shippingService.createShipment(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo vận đơn thành công", shipment));
    }

    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<ApiResponse<ShipmentTrackingResponse>> trackShipment(@PathVariable String trackingCode) {
        ShipmentTrackingResponse response = shippingService.getShipmentTracking(trackingCode);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu vận đơn thành công", response));
    }

    @GetMapping("/sub-order/{subOrderId}")
    public ResponseEntity<ApiResponse<ShipmentTrackingResponse>> getShipmentBySubOrder(@PathVariable Long subOrderId) {
        ShipmentTrackingResponse response = shippingService.getShipmentTrackingBySubOrderId(subOrderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vận đơn thành công", response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Shipment>> updateStatus(
            @PathVariable Long id,
            @RequestBody ShipmentStatusUpdateRequest request) {
        Shipment shipment = shippingService.updateShipmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái vận đơn thành công", shipment));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleShippingWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received shipping webhook: carrier={}, eventId={}, trackingCode={}, status={}",
                payload.getCarrierName(), payload.getEventId(), payload.getTrackingCode(), payload.getStatus());

        boolean processed = shippingService.handleWebhook(payload);
        if (!processed) {
            return ResponseEntity.ok(ApiResponse.success("Sự kiện đã được xử lý trước đó (Idempotent response)", payload.getEventId()));
        }
        return ResponseEntity.ok(ApiResponse.success("Xử lý webhook vận chuyển thành công", payload.getEventId()));
    }

    @PostMapping("/simulate-step/{trackingCode}")
    public ResponseEntity<ApiResponse<Shipment>> simulateNextStep(@PathVariable String trackingCode) {
        Shipment shipment = shippingService.simulateNextCarrierStep(trackingCode);
        return ResponseEntity.ok(ApiResponse.success("Mô phỏng bước vận chuyển thành công", shipment));
    }

    @PostMapping("/simulate-status/{trackingCode}")
    public ResponseEntity<ApiResponse<Shipment>> simulateStatus(
            @PathVariable String trackingCode,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) ShipmentStatusUpdateRequest request) {
        String targetStatus = status;
        if ((targetStatus == null || targetStatus.isBlank()) && request != null) {
            targetStatus = request.getStatus();
        }
        Shipment shipment = shippingService.simulateCarrierStatus(trackingCode, targetStatus);
        return ResponseEntity.ok(ApiResponse.success("Mô phỏng trạng thái đơn vị vận chuyển thành công", shipment));
    }

    @Data
    public static class WebhookPayload {
        private String eventId;
        private String trackingCode;
        private String carrierName;
        private String status;
    }
}
