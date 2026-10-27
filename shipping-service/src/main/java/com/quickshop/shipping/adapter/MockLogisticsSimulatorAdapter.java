package com.quickshop.shipping.adapter;

import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import com.quickshop.shipping.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * =========================================================================
 * MOCK LOGISTICS SIMULATOR ADAPTER (GIẢ LẬP ĐƠN VỊ VẬN CHUYỂN)
 * =========================================================================
 * Sử dụng trong môi trường thử nghiệm (Local / CI / Staging / Sandbox).
 * Tuyệt đối không sinh dữ liệu giả trong môi trường Production theo SRS 2.2.0.
 */
@Component("mockLogisticsSimulator")
@RequiredArgsConstructor
@Slf4j
public class MockLogisticsSimulatorAdapter implements CarrierAdapter {

    private final ShippingFeeService shippingFeeService;

    @Override
    public String getCarrierName() {
        return "MOCK_CARRIER";
    }

    @Override
    public boolean isMock() {
        return true;
    }

    @Override
    public CalculateFeeResponse calculateFee(CalculateFeeRequest request) {
        log.info("[MOCK LOGISTICS SIMULATOR] Calculating shipping fee for request: from={}, to={}",
                request.getFromProvince(), request.getToProvince());
        CalculateFeeResponse response = shippingFeeService.calculate(request);
        response.setIsMock(true);
        response.setCarrierName("MOCK_GHN");
        return response;
    }

    @Override
    public CarrierShipmentResult createShipment(CreateShipmentRequest request) {
        String carrier = request.getCarrierName() != null ? request.getCarrierName().toUpperCase() : "GHN";
        String trackingCode = "MOCK_" + carrier + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        CalculateFeeRequest feeRequest = CalculateFeeRequest.builder()
                .originAddress(request.getOriginAddress())
                .destinationAddress(request.getShippingAddress())
                .weightGrams(request.getWeightGrams())
                .carrierName(carrier)
                .build();
        CalculateFeeResponse feeResponse = shippingFeeService.calculate(feeRequest);

        log.info("[MOCK LOGISTICS SIMULATOR] Created mock shipment: subOrderId={}, trackingCode={}, fee={}",
                request.getSubOrderId(), trackingCode, feeResponse.getShippingFee());

        return CarrierShipmentResult.builder()
                .trackingCode(trackingCode)
                .carrierName(carrier)
                .shippingFee(feeResponse.getShippingFee())
                .status(ShippingStatus.READY_TO_PICK)
                .isMock(true)
                .rawResponse("{\"status\": \"SIMULATED_SUCCESS\", \"tracking_code\": \"" + trackingCode + "\"}")
                .build();
    }

    @Override
    public boolean cancelShipment(String trackingCode) {
        log.info("[MOCK LOGISTICS SIMULATOR] Cancelled shipment: trackingCode={}", trackingCode);
        return true;
    }

    @Override
    public boolean verifyWebhook(Map<String, String> headers, String rawPayload) {
        if (headers == null) {
            return true;
        }
        // Kiểm tra tín hiệu fail xác thực giả lập cho test case
        if ("true".equalsIgnoreCase(headers.get("x-mock-fail-auth")) ||
            "true".equalsIgnoreCase(headers.get("X-Mock-Fail-Auth"))) {
            log.warn("[MOCK LOGISTICS SIMULATOR] Webhook verification failed due to X-Mock-Fail-Auth header");
            return false;
        }

        String token = headers.getOrDefault("token", headers.get("Token"));
        if ("invalid_secret".equals(token)) {
            log.warn("[MOCK LOGISTICS SIMULATOR] Webhook verification failed: invalid token provided");
            return false;
        }

        log.info("[MOCK LOGISTICS SIMULATOR] Webhook signature verified successfully");
        return true;
    }
}
