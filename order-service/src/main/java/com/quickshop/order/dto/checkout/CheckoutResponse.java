package com.quickshop.order.dto.checkout;

import com.quickshop.order.entity.MasterOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả sau khi thực hiện Checkout thành công.
 * Trả về thông tin MasterOrder, danh sách SubOrders theo từng Shop và OrderItems snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse implements Serializable {

    private Long masterOrderId;
    private String orderCode;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFeeTotal;
    private BigDecimal voucherDiscount;
    private MasterOrderStatus status;
    private String paymentMethod;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String note;
    private Integer totalShops;
    private Integer totalItems;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<SubOrderResponse> subOrders = new ArrayList<>();
}

