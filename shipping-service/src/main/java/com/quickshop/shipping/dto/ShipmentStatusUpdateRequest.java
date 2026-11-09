package com.quickshop.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateRequest {
    private String status; // READY_TO_PICK, PICKED, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNED
    private String note;
    private String currentLocation;
}
