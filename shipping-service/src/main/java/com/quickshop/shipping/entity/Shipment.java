package com.quickshop.shipping.entity;

import com.quickshop.shipping.entity.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipments_sub_order", columnList = "sub_order_id", unique = true),
        @Index(name = "idx_shipments_tracking_code", columnList = "tracking_code", unique = true),
        @Index(name = "idx_shipments_status", columnList = "shipping_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "tracking_code", nullable = false, length = 50, unique = true)
    private String trackingCode;

    @Column(name = "carrier_name", nullable = false, length = 50)
    private String carrierName;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 50)
    @Builder.Default
    private ShippingStatus shippingStatus = ShippingStatus.READY_TO_PICK;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "origin_address", columnDefinition = "TEXT")
    private String originAddress;

    @Column(name = "weight_grams", nullable = false)
    @Builder.Default
    private Integer weightGrams = 500;

    @Column(name = "cod_amount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal codAmount = BigDecimal.ZERO;

    @Column(name = "is_mock", nullable = false)
    @Builder.Default
    private Boolean isMock = false;

    @Column(name = "status_note", length = 255)
    private String statusNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
