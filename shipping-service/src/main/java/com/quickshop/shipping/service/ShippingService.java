package com.quickshop.shipping.service;

import com.quickshop.shipping.controller.ShippingController;
import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.dto.ShipmentStatusUpdateRequest;
import com.quickshop.shipping.entity.Shipment;

public interface ShippingService {
    CalculateFeeResponse calculateFee(CalculateFeeRequest request);
    Shipment createShipment(CreateShipmentRequest request);
    Shipment getShipmentByTrackingCode(String trackingCode);
    Shipment getShipmentBySubOrderId(Long subOrderId);
    Shipment updateShipmentStatus(Long shipmentId, ShipmentStatusUpdateRequest request);
    boolean handleWebhook(ShippingController.WebhookPayload payload);
    Shipment simulateNextCarrierStep(String trackingCode);
}
