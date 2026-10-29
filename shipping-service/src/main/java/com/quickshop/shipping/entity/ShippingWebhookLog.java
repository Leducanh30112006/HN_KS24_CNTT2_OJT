package com.quickshop.shipping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shipping_webhooks_log")
@EntityListeners(AuditingEntityListener.class)
public class ShippingWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "carrier_name", nullable = false, length = 50)
    private String carrierName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @CreatedDate
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
