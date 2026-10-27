package com.quickshop.order.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quickshop.order.entity.enums.MasterOrderStatus;
import com.quickshop.order.entity.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterOrderResponse implements Serializable {

    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal voucherDiscount;
    private String voucherCode;
    private BigDecimal shippingFeeTotal;
    private MasterOrderStatus status;
    private PaymentMethod paymentMethod;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private List<SubOrderResponse> subOrders;

    /**
     * Đánh dấu nếu kết quả trả về là do request lặp (Idempotent replay).
     */
    private boolean duplicateRequest;
}
