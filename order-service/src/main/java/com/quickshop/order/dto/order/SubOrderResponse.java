package com.quickshop.order.dto.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quickshop.order.entity.enums.SubOrderStatus;
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
public class SubOrderResponse implements Serializable {

    private Long id;
    private Long masterOrderId;
    private Long shopId;
    private String shopName;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFee;
    private BigDecimal shopVoucherDiscount;
    private String shopVoucherCode;
    private BigDecimal finalAmount;
    private SubOrderStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;
}
