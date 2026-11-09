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
public class CalculateFeeResponse {
    private String carrier;
    private String serviceName;
    private BigDecimal shippingFee;
    private Integer estimatedDays;
    private String note;
}
