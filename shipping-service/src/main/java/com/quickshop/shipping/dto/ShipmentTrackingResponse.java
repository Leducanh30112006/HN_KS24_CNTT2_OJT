package com.quickshop.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingResponse {
    private Long id;
    private Long subOrderId;
    private String trackingCode;
    private String carrierName;
    private BigDecimal shippingFee;
    private String shippingStatus;
    private String statusDescription;
    private String recipientName;
    private String recipientPhone;
    private String fromAddress;
    private String toAddress;
    private Integer weightGrams;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TrackingMilestoneDto> timeline;
}

