package com.quickshop.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "escrow_accounts")
public class EscrowAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_order_id", nullable = false, unique = true)
    private Long subOrderId;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "commission_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal commissionFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "HOLDING"; // HOLDING, RELEASED, REFUNDED
}
