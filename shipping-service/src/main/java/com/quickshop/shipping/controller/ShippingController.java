package com.quickshop.shipping.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.dto.ShipmentStatusUpdateRequest;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.service.ShippingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Shipment>>> getAllShipments() {
        List<Shipment> shipments = shippingService.getAllShipments();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tất cả vận đơn thành công", shipments));
    }

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
    public ResponseEntity<ApiResponse<Shipment>> trackShipment(@PathVariable String trackingCode) {
        Shipment shipment = shippingService.getShipmentByTrackingCode(trackingCode);
        return ResponseEntity.ok(ApiResponse.success("Tra cứu vận đơn thành công", shipment));
    }

    @GetMapping("/sub-order/{subOrderId}")
    public ResponseEntity<ApiResponse<Shipment>> getShipmentBySubOrder(@PathVariable Long subOrderId) {
        Shipment shipment = shippingService.getShipmentBySubOrderId(subOrderId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vận đơn thành công", shipment));
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

    @Data
    public static class WebhookPayload {
        private String eventId;
        private String trackingCode;
        private String carrierName;
        private String status;
    }
}
