package com.quickshop.payment.entity;

import com.quickshop.payment.entity.enums.PaymentMethod;
import com.quickshop.payment.entity.enums.PaymentStatus;
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
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_txn_ref", columnList = "txn_ref", unique = true),
        @Index(name = "idx_order_id", columnList = "order_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 100)
    private String txnRef;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "vnpay_transaction_no", length = 50)
    private String vnpayTransactionNo;

    @Column(name = "vnpay_bank_code", length = 20)
    private String vnpayBankCode;

    @Column(name = "vnpay_bank_tran_no", length = 100)
    private String vnpayBankTranNo;

    @Column(name = "vnpay_card_type", length = 20)
    private String vnpayCardType;

    @Column(name = "vnpay_pay_date", length = 20)
    private String vnpayPayDate;

    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "vnpay_transaction_status", length = 10)
    private String vnpayTransactionStatus;

    @Column(name = "order_info", length = 255)
    private String orderInfo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
