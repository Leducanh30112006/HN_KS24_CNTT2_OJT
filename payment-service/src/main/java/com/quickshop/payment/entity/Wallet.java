package com.quickshop.payment.entity;

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
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_shop_id", columnList = "shop_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false, unique = true)
    private Long shopId;

    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 12, scale = 0)
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "frozen_balance", nullable = false, precision = 12, scale = 0)
    private BigDecimal frozenBalance = BigDecimal.ZERO;

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
