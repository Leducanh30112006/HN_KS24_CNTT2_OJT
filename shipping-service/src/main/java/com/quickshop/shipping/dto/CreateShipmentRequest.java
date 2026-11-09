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
public class CreateShipmentRequest {
    private Long subOrderId;
    private String carrierName; // GHN, GHTK, MOCK_CARRIER
    private BigDecimal shippingFee;
    private String recipientName;
    private String recipientPhone;
    private String fromAddress;
    private String toAddress;
    private Integer weightGrams;
}
