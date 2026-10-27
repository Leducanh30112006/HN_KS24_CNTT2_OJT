package com.quickshop.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event phát ra khi trạng thái vận đơn (Shipment) thay đổi trong Shipping & Logistics Service.
 * Order Service và các service khác lắng nghe để cập nhật trạng thái Sub-Order tương ứng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdatedEvent implements Serializable {

    private Long subOrderId;
    private String trackingCode;
    private String carrierName;
    private String shippingStatus; // READY_TO_PICK, PICKED, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNED
    private BigDecimal shippingFee;
    private String note;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
