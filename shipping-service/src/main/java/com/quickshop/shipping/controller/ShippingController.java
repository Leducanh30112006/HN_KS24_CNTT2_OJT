package com.quickshop.shipping.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.shipping.entity.Shipment;
import com.quickshop.shipping.entity.ShippingWebhookLog;
import com.quickshop.shipping.repository.ShipmentRepository;
import com.quickshop.shipping.repository.ShippingWebhookLogRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShipmentRepository shipmentRepository;
    private final ShippingWebhookLogRepository webhookLogRepository;

    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<ApiResponse<Shipment>> trackShipment(@PathVariable String trackingCode) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingCode(trackingCode);
        if (shipmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(shipmentOpt.get()));
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<ApiResponse<String>> handleShippingWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received shipping webhook from {}: eventId={}, trackingCode={}, status={}",
                payload.getCarrierName(), payload.getEventId(), payload.getTrackingCode(), payload.getStatus());

        // 1. Kiểm tra tính lũy trùng Idempotency bằng event_id
        if (webhookLogRepository.existsByEventId(payload.getEventId())) {
            log.warn("Duplicate webhook detected for eventId: {}, skipping processing", payload.getEventId());
            return ResponseEntity.ok(ApiResponse.success("Sự kiện đã được xử lý trước đó (Idempotent response)", payload.getEventId()));
        }

        // 2. Ghi nhận log webhook mới
        webhookLogRepository.save(ShippingWebhookLog.builder()
                .eventId(payload.getEventId())
                .carrierName(payload.getCarrierName())
                .payload(payload.toString())
                .build());

        // 3. Cập nhật trạng thái vận đơn
        shipmentRepository.findByTrackingCode(payload.getTrackingCode()).ifPresent(shipment -> {
            shipment.setShippingStatus(payload.getStatus());
            shipmentRepository.save(shipment);
            log.info("Updated shipment trackingCode {} to status {}", payload.getTrackingCode(), payload.getStatus());
        });

        return ResponseEntity.ok(ApiResponse.success("Xử lý webhook vận chuyển thành công", payload.getEventId()));
    }

    @Data
    public static class WebhookPayload {
        private String eventId;
        private String trackingCode;
        private String carrierName;
        private String status;
    }
}
