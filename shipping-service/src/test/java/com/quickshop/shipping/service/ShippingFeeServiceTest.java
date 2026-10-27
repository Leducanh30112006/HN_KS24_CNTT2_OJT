package com.quickshop.shipping.service;

import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShippingFeeService Tests - Tính phí vận chuyển theo SRS QuickShop 2.2.0")
class ShippingFeeServiceTest {

    private ShippingFeeService feeService;

    @BeforeEach
    void setUp() {
        feeService = new ShippingFeeService();
        ReflectionTestUtils.setField(feeService, "baseIntraProvince", BigDecimal.valueOf(22000));
        ReflectionTestUtils.setField(feeService, "baseIntraRegion", BigDecimal.valueOf(30000));
        ReflectionTestUtils.setField(feeService, "baseInterRegion", BigDecimal.valueOf(40000));
        ReflectionTestUtils.setField(feeService, "baseWeightGrams", 1000);
        ReflectionTestUtils.setField(feeService, "stepWeightGrams", 500);
        ReflectionTestUtils.setField(feeService, "stepWeightFee", BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("Nội thành / Nội tỉnh (Hà Nội -> Hà Nội, <= 1000g) => 22.000 VNĐ")
    void testCalculate_IntraProvince_StandardWeight() {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hà Nội")
                .weightGrams(500)
                .build();

        CalculateFeeResponse response = feeService.calculate(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(22000), response.getShippingFee());
        assertEquals("INTRA_PROVINCE", response.getDistanceType());
        assertEquals("1-2 ngày", response.getEstimatedDeliveryDays());
    }

    @Test
    @DisplayName("Liên tỉnh cùng miền (Hà Nội -> Hải Phòng, <= 1000g) => 30.000 VNĐ")
    void testCalculate_IntraRegion_StandardWeight() {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hải Phòng")
                .weightGrams(800)
                .build();

        CalculateFeeResponse response = feeService.calculate(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(30000), response.getShippingFee());
        assertEquals("INTRA_REGION", response.getDistanceType());
        assertEquals("2-3 ngày", response.getEstimatedDeliveryDays());
    }

    @Test
    @DisplayName("Liên miền Bắc - Nam (Hà Nội -> TP. Hồ Chí Minh, <= 1000g) => 40.000 VNĐ")
    void testCalculate_InterRegion_StandardWeight() {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hồ Chí Minh")
                .weightGrams(900)
                .build();

        CalculateFeeResponse response = feeService.calculate(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(40000), response.getShippingFee());
        assertEquals("INTER_REGION", response.getDistanceType());
        assertEquals("3-5 ngày", response.getEstimatedDeliveryDays());
    }

    @Test
    @DisplayName("Phụ phí vượt cân: Hà Nội -> Hà Nội, 1500g (vượt 500g => +5.000 VNĐ) => 27.000 VNĐ")
    void testCalculate_Overweight_Surcharge() {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hà Nội")
                .weightGrams(1500)
                .build();

        CalculateFeeResponse response = feeService.calculate(request);

        assertNotNull(response);
        // 22.000 + 5.000 = 27.000
        assertEquals(BigDecimal.valueOf(27000), response.getShippingFee());
        assertEquals(1500, response.getChargeableWeightGrams());
    }

    @Test
    @DisplayName("Quy đổi thể tích hàng cồng kềnh: L=30, W=20, H=15 => (30*20*15*1000)/5000 = 1800g > 500g")
    void testCalculate_VolumetricWeight_TakesPrecedence() {
        CalculateFeeRequest request = CalculateFeeRequest.builder()
                .fromProvince("Hà Nội")
                .toProvince("Hà Nội")
                .weightGrams(500)
                .lengthCm(30)
                .widthCm(20)
                .heightCm(15)
                .build();

        CalculateFeeResponse response = feeService.calculate(request);

        assertNotNull(response);
        // Trọng lượng quy đổi = (30 * 20 * 15 * 1000) / 5000 = 1800g
        // Vượt mức 1000g là 800g => 2 bước x 5000 = 10.000
        // Tổng phí = 22.000 + 10.000 = 32.000
        assertEquals(1800, response.getChargeableWeightGrams());
        assertEquals(BigDecimal.valueOf(32000), response.getShippingFee());
    }
}
