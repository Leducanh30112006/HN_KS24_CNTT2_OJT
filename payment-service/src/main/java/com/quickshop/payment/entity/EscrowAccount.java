package com.quickshop.payment.entity;

import com.quickshop.payment.entity.enums.EscrowStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "escrow_accounts", indexes = {
        @Index(name = "idx_escrow_sub_order", columnList = "sub_order_id", unique = true),
        @Index(name = "idx_escrow_master_order", columnList = "master_order_id"),
        @Index(name = "idx_escrow_shop_id", columnList = "shop_id"),
        @Index(name = "idx_escrow_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(name = "master_order_id", nullable = false)
    private Long masterOrderId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "commission_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal commissionFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EscrowStatus status;

    @Builder.Default
    @Column(name = "is_disputed", nullable = false)
    private boolean isDisputed = false;

    @Column(name = "dispute_reason", length = 255)
    private String disputeReason;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
