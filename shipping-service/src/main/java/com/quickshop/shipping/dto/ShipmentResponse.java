package com.quickshop.shipping.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quickshop.shipping.entity.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    private Long id;
    private Long subOrderId;
    private Long shopId;
    private String trackingCode;
    private String carrierName;
    private BigDecimal shippingFee;
    private ShippingStatus shippingStatus;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String originAddress;
    private Integer weightGrams;
    private BigDecimal codAmount;
    private Boolean isMock;
    private String statusNote;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
