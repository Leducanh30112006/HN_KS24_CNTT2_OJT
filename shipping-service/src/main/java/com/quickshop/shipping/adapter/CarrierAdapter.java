package com.quickshop.shipping.adapter;

import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;

import java.util.Map;

/**
 * Interface cho các adapter kết nối đơn vị vận chuyển (GHN, GHTK hoặc Mock Logistics Simulator).
 */
public interface CarrierAdapter {

    String getCarrierName();

    boolean isMock();

    CalculateFeeResponse calculateFee(CalculateFeeRequest request);

    CarrierShipmentResult createShipment(CreateShipmentRequest request);

    boolean cancelShipment(String trackingCode);

    /**
     * Xác minh tính hợp lệ và nguồn gốc của Webhook từ đơn vị vận chuyển.
     * Kiểm tra chữ ký bảo mật (HMAC/Token) dựa trên cấu hình bảo mật của hãng.
     */
    boolean verifyWebhook(Map<String, String> headers, String rawPayload);
}
