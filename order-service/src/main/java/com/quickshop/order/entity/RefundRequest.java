package com.quickshop.order.entity;

import com.quickshop.order.entity.enums.RefundRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "evidence_images", columnDefinition = "TEXT")
    private String evidenceImages;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RefundRequestStatus status = RefundRequestStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
