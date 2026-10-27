package com.quickshop.order.entity;

import com.quickshop.order.entity.enums.SubOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_order_id", nullable = false)
    private MasterOrder masterOrder;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "shop_voucher_discount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shopVoucherDiscount = BigDecimal.ZERO;

    @Column(name = "shop_voucher_code", length = 50)
    private String shopVoucherCode;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubOrderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setSubOrder(this);
    }
}
