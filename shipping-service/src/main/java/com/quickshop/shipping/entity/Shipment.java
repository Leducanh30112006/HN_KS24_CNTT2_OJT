package com.quickshop.shipping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shipments")
@EntityListeners(AuditingEntityListener.class)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(name = "tracking_code", nullable = false, unique = true, length = 50)
    private String trackingCode;

    @Column(name = "carrier_name", nullable = false, length = 50)
    private String carrierName; // GHN, GHTK, MOCK_CARRIER

    @Builder.Default
    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "shipping_status", nullable = false, length = 50)
    private String shippingStatus = "READY_TO_PICK"; // READY_TO_PICK, PICKED, DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNED

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
