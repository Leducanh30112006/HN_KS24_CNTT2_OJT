package com.quickshop.payment.entity;

import com.quickshop.payment.entity.enums.LedgerTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ledger_account_debit", columnList = "account_debit"),
        @Index(name = "idx_ledger_account_credit", columnList = "account_credit"),
        @Index(name = "idx_ledger_type", columnList = "transaction_type")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_ledger_tx_accounts", columnNames = {"transaction_id", "transaction_type", "account_debit", "account_credit"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private LedgerTransactionType transactionType;

    @Column(name = "account_debit", nullable = false, length = 50)
    private String accountDebit;

    @Column(name = "account_credit", nullable = false, length = 50)
    private String accountCredit;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
