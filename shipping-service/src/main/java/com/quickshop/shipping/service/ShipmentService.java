package com.quickshop.shipping.service;

import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.entity.enums.ShippingStatus;

import java.util.List;
import java.util.Map;

public interface ShipmentService {

    CalculateFeeResponse calculateShippingFee(CalculateFeeRequest request);

    ShipmentResponse createShipment(CreateShipmentRequest request);

    ShipmentResponse getShipmentBySubOrderId(Long subOrderId);

    ShipmentResponse getShipmentByTrackingCode(String trackingCode);

    List<ShipmentResponse> getShipmentsByShopId(Long shopId);

    ShipmentResponse updateShippingStatus(String trackingCode, ShippingStatus newStatus, String note);

    /**
     * Tiếp nhận và xử lý an toàn Webhook từ đơn vị vận chuyển.
     * Áp dụng xác thực nguồn gửi, kiểm tra race condition với Redis Lock + DB Unique Constraint,
     * xác thực chuyển đổi trạng thái hợp lệ và phát sự kiện đồng bộ Order Service.
     */
    void processWebhook(String carrier, Map<String, String> headers, String rawPayload, CarrierWebhookRequest request);
}
