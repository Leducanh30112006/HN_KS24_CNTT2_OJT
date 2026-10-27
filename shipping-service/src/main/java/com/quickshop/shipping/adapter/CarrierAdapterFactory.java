package com.quickshop.shipping.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CarrierAdapterFactory {

    private final MockLogisticsSimulatorAdapter mockLogisticsSimulatorAdapter;
    private final GhnCarrierAdapter ghnCarrierAdapter;

    @Value("${quickshop.shipping.carrier.mode:mock}")
    private String carrierMode;

    @Value("${quickshop.shipping.carrier.default-carrier:GHN}")
    private String defaultCarrier;

    /**
     * Lấy adapter phù hợp dựa trên cấu hình môi trường và tên hãng vận chuyển.
     * Nếu carrierMode = 'mock', luôn trả về MockLogisticsSimulatorAdapter để an toàn.
     */
    public CarrierAdapter getAdapter(String carrierName) {
        String carrier = (carrierName != null && !carrierName.isBlank()) ? carrierName.trim().toUpperCase() : defaultCarrier.toUpperCase();

        if ("mock".equalsIgnoreCase(carrierMode) || "MOCK_CARRIER".equalsIgnoreCase(carrier) || carrier.startsWith("MOCK")) {
            log.debug("Using MockLogisticsSimulatorAdapter (mode={}, carrier={})", carrierMode, carrier);
            return mockLogisticsSimulatorAdapter;
        }

        if ("GHN".equalsIgnoreCase(carrier)) {
            return ghnCarrierAdapter;
        }

        // Mặc định trả về mock nếu ở chế độ mock
        return mockLogisticsSimulatorAdapter;
    }
}
