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

    private BigDecimal shippingFee;
    private Integer chargeableWeightGrams;
    private String distanceType; // INTRA_PROVINCE, INTRA_REGION, INTER_REGION
    private String estimatedDeliveryDays;
    private String carrierName;
    private Boolean isMock;
}
