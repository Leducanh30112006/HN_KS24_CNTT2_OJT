package com.quickshop.payment.entity;

import com.quickshop.payment.entity.enums.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals", indexes = {
        @Index(name = "idx_withdrawal_shop_id", columnList = "shop_id"),
        @Index(name = "idx_withdrawal_status", columnList = "status"),
        @Index(name = "idx_withdrawal_idempotency", columnList = "idempotency_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "bank_account_number", nullable = false, length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", nullable = false, length = 100)
    private String bankAccountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "bank_transaction_ref", length = 100)
    private String bankTransactionRef;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
