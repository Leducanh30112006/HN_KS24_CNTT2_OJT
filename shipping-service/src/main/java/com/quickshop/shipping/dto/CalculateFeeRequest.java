package com.quickshop.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateFeeRequest {
    private String carrier; // GHN, GHTK, MOCK_CARRIER
    private String fromProvince;
    private String toProvince;
    private Integer weightGrams;
    private BigDecimal orderValue;
    private Boolean isExpress;
}
