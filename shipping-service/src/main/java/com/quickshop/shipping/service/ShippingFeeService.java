package com.quickshop.shipping.service;

import com.quickshop.shipping.dto.CalculateFeeRequest;
import com.quickshop.shipping.dto.CalculateFeeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class ShippingFeeService {

    @Value("${quickshop.shipping.pricing.base-intra-province:22000}")
    private BigDecimal baseIntraProvince;

    @Value("${quickshop.shipping.pricing.base-intra-region:30000}")
    private BigDecimal baseIntraRegion;

    @Value("${quickshop.shipping.pricing.base-inter-region:40000}")
    private BigDecimal baseInterRegion;

    @Value("${quickshop.shipping.pricing.base-weight-grams:1000}")
    private int baseWeightGrams;

    @Value("${quickshop.shipping.pricing.step-weight-grams:500}")
    private int stepWeightGrams;

    @Value("${quickshop.shipping.pricing.step-weight-fee:5000}")
    private BigDecimal stepWeightFee;

    // Phân vùng địa lý 3 miền Việt Nam
    private static final Set<String> NORTH_PROVINCES = new HashSet<>(Arrays.asList(
            "hà nội", "ha noi", "hải phòng", "hai phong", "quảng ninh", "quang ninh",
            "bắc ninh", "bac ninh", "hải dương", "hai duong", "hưng yên", "hung yen",
            "thái nguyên", "thai nguyen", "nam định", "nam dinh", "vĩnh phúc", "vinh phuc",
            "phú thọ", "phu tho", "bắc giang", "bac giang", "ninh bình", "ninh binh",
            "hà nam", "ha nam", "thái bình", "thai binh", "lạng sơn", "lang son",
            "sơn la", "son la", "hòa bình", "hoa binh", "yên bái", "yen bai",
            "lào cai", "lao cai", "tuyên quang", "tuyen quang", "hà giang", "ha giang"
    ));

    private static final Set<String> CENTRAL_PROVINCES = new HashSet<>(Arrays.asList(
            "đà nẵng", "da nang", "thừa thiên huế", "thua thien hue", "huế", "hue",
            "quảng nam", "quang nam", "quảng ngãi", "quang ngai", "bình định", "binh dinh",
            "phú yên", "phu yen", "khánh hòa", "khanh hoa", "nha trang", "ninh thuận",
            "ninh thuan", "bình thuận", "binh thuan", "thanh hóa", "thanh hoa",
            "nghệ an", "nghe an", "hà tĩnh", "ha tinh", "quảng bình", "quang binh",
            "quảng trị", "quang tri", "kon tum", "gia lai", "đắk lắk", "dak lak", "đắk nông", "lâm đồng", "da lat"
    ));

    private static final Set<String> SOUTH_PROVINCES = new HashSet<>(Arrays.asList(
            "hồ chí minh", "ho chi minh", "tp.hcm", "tphcm", "sài gòn", "sai gon",
            "bình dương", "binh duong", "đồng nai", "dong nai", "bà rịa - vũng tàu",
            "vũng tàu", "vung tau", "long an", "tiền giang", "tien giang",
            "bến tre", "ben tre", "trà vinh", "tra vinh", "vĩnh long", "vinh long",
            "đồng tháp", "dong thap", "an giang", "kiên giang", "kien giang",
            "cần thơ", "can tho", "hậu giang", "hau giang", "sóc trăng", "soc trang",
            "bạc liêu", "bac lieu", "cà mau", "ca mau", "tây ninh", "tay ninh", "bình phước"
    ));

    /**
     * Tính phí vận chuyển dựa trên địa chỉ nguồn, đích, trọng lượng thực và kích thước gói hàng.
     */
    public CalculateFeeResponse calculate(CalculateFeeRequest request) {
        String from = extractProvince(request.getFromProvince(), request.getOriginAddress());
        String to = extractProvince(request.getToProvince(), request.getDestinationAddress());

        String distanceType = determineDistanceType(from, to);
        BigDecimal baseFee = switch (distanceType) {
            case "INTRA_PROVINCE" -> baseIntraProvince;
            case "INTRA_REGION" -> baseIntraRegion;
            default -> baseInterRegion;
        };

        // Tính trọng lượng tính cước (Chargeable Weight): max(cân nặng thực, quy đổi kích thước L*W*H/5000)
        int actualWeight = request.getWeightGrams() != null ? request.getWeightGrams() : 500;
        int volumetricWeight = 0;
        if (request.getLengthCm() != null && request.getWidthCm() != null && request.getHeightCm() != null) {
            volumetricWeight = (request.getLengthCm() * request.getWidthCm() * request.getHeightCm() * 1000) / 5000;
        }
        int chargeableWeight = Math.max(actualWeight, volumetricWeight);

        // Phụ phí vượt cân: mỗi 500g vượt mức chuẩn 1000g tính thêm phí step
        BigDecimal weightSurcharge = BigDecimal.ZERO;
        if (chargeableWeight > baseWeightGrams) {
            int extraGrams = chargeableWeight - baseWeightGrams;
            int steps = (int) Math.ceil((double) extraGrams / stepWeightGrams);
            weightSurcharge = stepWeightFee.multiply(BigDecimal.valueOf(steps));
        }

        BigDecimal totalFee = baseFee.add(weightSurcharge);

        String deliveryDays = switch (distanceType) {
            case "INTRA_PROVINCE" -> "1-2 ngày";
            case "INTRA_REGION" -> "2-3 ngày";
            default -> "3-5 ngày";
        };

        log.debug("Calculated shipping fee: from='{}', to='{}', type={}, weight={}g, totalFee={}",
                from, to, distanceType, chargeableWeight, totalFee);

        return CalculateFeeResponse.builder()
                .shippingFee(totalFee)
                .chargeableWeightGrams(chargeableWeight)
                .distanceType(distanceType)
                .estimatedDeliveryDays(deliveryDays)
                .carrierName(request.getCarrierName() != null ? request.getCarrierName() : "GHN")
                .isMock(true)
                .build();
    }

    public String determineDistanceType(String fromProvince, String toProvince) {
        if (fromProvince == null || toProvince == null || fromProvince.isBlank() || toProvince.isBlank()) {
            return "INTRA_REGION"; // Fallback mặc định
        }

        String p1 = fromProvince.trim().toLowerCase();
        String p2 = toProvince.trim().toLowerCase();

        // 1. Cùng một tỉnh/thành phố
        if (p1.equals(p2) || p1.contains(p2) || p2.contains(p1)) {
            return "INTRA_PROVINCE";
        }

        // 2. Xác định miền
        int region1 = getRegionCode(p1);
        int region2 = getRegionCode(p2);

        if (region1 != 0 && region1 == region2) {
            return "INTRA_REGION";
        }

        return "INTER_REGION";
    }

    private int getRegionCode(String province) {
        for (String p : NORTH_PROVINCES) {
            if (province.contains(p)) return 1; // Miền Bắc
        }
        for (String p : CENTRAL_PROVINCES) {
            if (province.contains(p)) return 2; // Miền Trung
        }
        for (String p : SOUTH_PROVINCES) {
            if (province.contains(p)) return 3; // Miền Nam
        }
        return 0; // Không xác định
    }

    private String extractProvince(String province, String fullAddress) {
        if (province != null && !province.isBlank()) {
            return province.trim();
        }
        if (fullAddress == null || fullAddress.isBlank()) {
            return "";
        }
        // Thử tìm tỉnh/thành trong chuỗi địa chỉ
        String addrLower = fullAddress.toLowerCase();
        for (String p : NORTH_PROVINCES) {
            if (addrLower.contains(p)) return p;
        }
        for (String p : SOUTH_PROVINCES) {
            if (addrLower.contains(p)) return p;
        }
        for (String p : CENTRAL_PROVINCES) {
            if (addrLower.contains(p)) return p;
        }
        // Lấy đoạn cuối sau dấu phẩy nếu có
        String[] parts = fullAddress.split(",");
        return parts[parts.length - 1].trim();
    }
}
