package com.quickshop.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi lịch sử sử dụng voucher của người dùng cho từng đơn hàng (MasterOrder hoặc SubOrder).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "voucher_usages")
@EntityListeners(AuditingEntityListener.class)
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "voucher_code", nullable = false, length = 50)
    private String voucherCode;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "master_order_id", nullable = false)
    private Long masterOrderId;

    @Column(name = "sub_order_id")
    private Long subOrderId; // null nếu là Voucher toàn sàn, có ID nếu là Voucher của Shop

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal discountAmount;

    @CreatedDate
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
}
