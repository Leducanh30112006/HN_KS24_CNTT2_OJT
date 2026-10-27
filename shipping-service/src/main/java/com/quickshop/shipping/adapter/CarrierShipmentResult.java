package com.quickshop.shipping.adapter;

import com.quickshop.shipping.entity.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierShipmentResult {

    private String trackingCode;
    private String carrierName;
    private BigDecimal shippingFee;
    private ShippingStatus status;
    private Boolean isMock;
    private String rawResponse;
}
