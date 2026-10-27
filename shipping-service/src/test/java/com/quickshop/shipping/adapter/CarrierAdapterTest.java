package com.quickshop.shipping.adapter;

import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import com.quickshop.shipping.dto.CreateShipmentRequest;
import com.quickshop.shipping.exception.CarrierConfigurationException;
import com.quickshop.shipping.service.ShippingFeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Carrier Adapter Tests - Phân biệt rõ môi trường Mock và Production")
class CarrierAdapterTest {

    @Mock
    private ShippingFeeService shippingFeeService;

    private MockLogisticsSimulatorAdapter mockAdapter;
    private GhnCarrierAdapter ghnAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockLogisticsSimulatorAdapter(shippingFeeService);
        ghnAdapter = new GhnCarrierAdapter(shippingFeeService);
    }

    @Test
    @DisplayName("Mock Adapter: Đánh dấu rõ là mô phỏng (isMock = true, tracking_code bắt đầu bằng MOCK_)")
    void testMockAdapter_CreatesSimulatedData() {
        when(shippingFeeService.calculate(any())).thenReturn(
                CalculateFeeResponse.builder().shippingFee(BigDecimal.valueOf(22000)).build()
        );

        CreateShipmentRequest request = CreateShipmentRequest.builder()
                .subOrderId(101L)
                .shopId(1L)
                .recipientName("Khách Hàng")
                .recipientPhone("0901234567")
                .shippingAddress("Hà Nội")
                .carrierName("GHN")
                .build();

        CarrierShipmentResult result = mockAdapter.createShipment(request);

        assertNotNull(result);
        assertTrue(result.getIsMock(), "Mock adapter phải luôn trả isMock = true");
        assertTrue(result.getTrackingCode().startsWith("MOCK_GHN_"));
        assertEquals(BigDecimal.valueOf(22000), result.getShippingFee());
    }

    @Test
    @DisplayName("Production Adapter: Nếu ở chế độ production mà thiếu API Token/Shop ID => ném CarrierConfigurationException")
    void testProductionAdapter_MissingCredentials_ThrowsException() {
        ReflectionTestUtils.setField(ghnAdapter, "carrierMode", "production");
        ReflectionTestUtils.setField(ghnAdapter, "apiToken", "");
        ReflectionTestUtils.setField(ghnAdapter, "shopId", "");

        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hồ Chí Minh")
                .build();

        CarrierConfigurationException ex = assertThrows(CarrierConfigurationException.class, () ->
                ghnAdapter.calculateFee(request));

        assertTrue(ex.getMessage().contains("Chưa cấu hình GHN API Token"));
    }
}
