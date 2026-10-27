package com.quickshop.shipping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipping_webhooks_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipping_webhooks_event_id", columnNames = {"event_id"})
        },
        indexes = {
                @Index(name = "idx_shipping_webhook_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_shipping_webhook_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100, unique = true)
    private String eventId;

    @Column(name = "carrier_name", nullable = false, length = 50)
    private String carrierName;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "SUCCESS"; // SUCCESS, SKIPPED_DUPLICATE, FAILED

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;
}
